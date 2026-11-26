#!/usr/bin/env bash
# E2E smoke для Bebringo.
#
# Что делает:
#   1. собирает и ставит APK (если передан --build)
#   2. чистит данные и запускает приложение
#   3. проходит онбординг, нажимая по тексту (не по координатам)
#   4. проверяет, что оказались на главном экране
#   5. снимает скриншоты ключевых экранов
#   6. ищет FATAL EXCEPTION в logcat
#
# Требует: adb в PATH, запущенный эмулятор или подключённый телефон,
# python3 для разбора uiautomator dump.

set -u

PACKAGE="org.dueattendant149.bebringo"
ACTIVITY="$PACKAGE/.MainActivity"
OUT_DIR="${OUT_DIR:-/tmp/opencode/e2e}"
BUILD=0
[ "${1:-}" = "--build" ] && BUILD=1

mkdir -p "$OUT_DIR"
FAILS=0

say() { printf '%s\n' "$*"; }
fail() { say "  ✗ $*"; FAILS=$((FAILS + 1)); }
ok()   { say "  ✓ $*"; }

require_device() {
  if ! adb get-state >/dev/null 2>&1; then
    say "Устройство не найдено. Запусти эмулятор или подключи телефон."
    exit 2
  fi
}

dump_ui() {
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml /tmp/opencode/ui.xml >/dev/null 2>&1
}

# Нажимает элемент по точному тексту. Возвращает 1, если не найден.
tap_text() {
  local text="$1"
  dump_ui
  local coords
  coords=$(python3 - "$text" <<'PY'
import sys, xml.etree.ElementTree as ET
target = sys.argv[1]
try:
    root = ET.parse('/tmp/opencode/ui.xml').getroot()
except Exception:
    sys.exit(1)
for node in root.iter('node'):
    if node.get('text', '').strip() == target:
        bounds = node.get('bounds', '')
        # [x1,y1][x2,y2]
        try:
            left_top, right_bottom = bounds.split('][')
            x1, y1 = left_top.strip('[').split(',')
            x2, y2 = right_bottom.strip(']').split(',')
            print(int((int(x1) + int(x2)) / 2), int((int(y1) + int(y2)) / 2))
            sys.exit(0)
        except Exception:
            continue
sys.exit(1)
PY
)
  if [ -z "${coords:-}" ]; then
    return 1
  fi
  adb shell input tap $coords >/dev/null
  sleep 0.6
  return 0
}

shot() {
  local name="$1"
  adb exec-out screencap -p > "$OUT_DIR/$name.png" 2>/dev/null
  if [ -s "$OUT_DIR/$name.png" ]; then ok "скриншот $name.png"; else fail "скриншот $name.png не снят"; fi
}

expect_text() {
  local text="$1"
  dump_ui
  if grep -q "text=\"$text\"" /tmp/opencode/ui.xml 2>/dev/null; then
    ok "на экране есть «$text»"
  else
    fail "не найден текст «$text»"
    return 1
  fi
}

step() { say ""; say "[$1]"; }

require_device

if [ "$BUILD" = "1" ]; then
  step "Сборка"
  ( cd "$(dirname "$0")/.." && gradle assembleDebug -q ) && ok "APK собран" || { fail "сборка"; exit 1; }
  adb install -r app/build/outputs/apk/debug/app-debug.apk >/dev/null 2>&1 \
    && ok "APK установлен" || fail "установка"
fi

step "Чистый старт"
adb shell pm clear "$PACKAGE" >/dev/null 2>&1
adb logcat -c
adb shell am start -n "$ACTIVITY" >/dev/null 2>&1
sleep 2
shot "01-onboarding"

step "Онбординг: цель"
tap_text "Путешествия" && ok "выбрана цель" || fail "нет варианта «Путешествия»"
tap_text "Дальше"      && ok "дальше" || fail "нет кнопки «Дальше»"
shot "02-anchor"

step "Онбординг: якорь"
tap_text "утром" && ok "время выбрано" || fail "нет чипа «утром»"
tap_text "Дальше" && ok "дальше" || fail "нет кнопки «Дальше»"
shot "03-placement"

step "Онбординг: плейсмент"
tap_text "Продолжить" && ok "плейсмент пропущен" || fail "нет кнопки «Продолжить»"
tap_text "Начать с третьего шага" && ok "форсаж" || fail "нет кнопки форсажа"
tap_text "Начать первый блок" && ok "финиш онбординга" || fail "нет кнопки «Начать первый блок»"
shot "04-home"

step "Главный экран"
expect_text "Bebringo" || true
expect_text "Продолжить" || true

step "Урок"
tap_text "Продолжить" && ok "урок открыт" || fail "не открылся урок"
shot "05-lesson"
tap_text "Проверить" && ok "ответ проверен" || fail "нет кнопки «Проверить»"
shot "06-feedback"

step "Перезапуск"
adb shell am force-stop "$PACKAGE"
adb shell am start -n "$ACTIVITY" >/dev/null 2>&1
sleep 2
if tap_text "Продолжить"; then ok "онбординг не показан повторно"; else fail "после перезапуска нет главного экрана"; fi
shot "07-restart"

step "Падения"
if adb logcat -d '*:E' | grep -q "FATAL EXCEPTION"; then
  fail "в логах есть FATAL EXCEPTION"
  adb logcat -d '*:E' | grep -A5 "FATAL EXCEPTION" | head -40
else
  ok "падений нет"
fi

say ""
if [ "$FAILS" -eq 0 ]; then
  say "ИТОГ: smoke пройден. Скриншоты: $OUT_DIR"
  exit 0
else
  say "ИТОГ: провалов $FAILS. Скриншоты: $OUT_DIR"
  exit 1
fi
