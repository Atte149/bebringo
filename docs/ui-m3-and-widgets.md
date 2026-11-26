# Дизайн-система Material Design 3, Маскот и Glance-виджеты

**Статус:** актуальная спецификация (v2)
**Связанные файлы:**
- [`Color.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/ui/theme/Color.kt)
- [`Theme.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/ui/theme/Theme.kt)
- [`Controls.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/ui/components/Controls.kt)
- [`Mascot.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/ui/components/Mascot.kt)
- [`BottomBar.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/ui/navigation/BottomBar.kt)
- [`Widgets.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/widget/Widgets.kt)

---

## 1. Цветовая система Material Design 3 / Material You

Bebringo использует тёплую органическую палитру **Material Design 3** с тональной иерархией поверхностей (`surfaceContainerLowest` → `surfaceContainerHighest`) вместо жёстких рамок и теней, а также поддерживает опциональный **Dynamic Color (Material You)** на Android 12+ (переключается в `Настройки → Тон и оформление`).

### 1.1 Семантические токены (`BebringoColors`)

| Токен | Светлая тема | Тёмная тема | Назначение |
|---|---|---|---|
| `bg` | `#FDF6EC` | `#141311` | Основной тёплый фон экрана |
| `surface` | `#FFFFFF` | `#1D1B18` | Базовая поверхность карточек |
| `surfaceContainerLowest` | `#FFFFFF` | `#0F0E0C` | Вложенные поля ввода и чистые холсты |
| `surfaceContainerLow` | `#FAF1E4` | `#1B1916` | Вторичные секции и фон сгруппированных списков |
| `surfaceContainer` | `#F5EBDC` | `#221F1B` | Контейнер нижней навигации и карточек меню |
| `surfaceContainerHigh` | `#EFE3D1` | `#2B2823` | Выделенные контейнеры, чипы, плитки слов |
| `surfaceContainerHighest` | `#E8DAC5` | `#36322C` | Нажатые состояния, неактивные переключатели |
| `primary` | `#2F5D3A` | `#B8D8A8` | Главный лесной зелёный акцент (CTA, активный прогресс) |
| `onPrimary` | `#FDF6EC` | `#16301C` | Текст и иконки на `primary` |
| `primaryContainer` | `#D4ECC8` | `#23472C` | Заливка выбранного индикатора в M3 `NavigationBar`, чипы активного выбора |
| `onPrimaryContainer` | `#0F2E17` | `#D4ECC8` | Текст внутри `primaryContainer` |
| `secondaryContainer` | `#E6E2D3` | `#35332A` | Мягкие информационные плашки и теги |
| `tertiaryContainer` | `#FCE8C8` | `#4A381C` | Бейджи стриков, вех и подсказок («Совет бобра») |
| `outline` | `#D9CEBC` | `#454039` | Внешние контуры карточек |
| `outlineVariant` | `#EAE0D0` | `#2F2B26` | Тонкие внутренние разделители в `GroupedListCard` |
| `error` / `errorSurface` | `#B4553C` / `#FAEDE4` | `#E08A6E` / `#2E211D` | Карточки разбора ошибок |
| `success` / `successContainer` | `#3F7A4C` / `#DCF2DF` | `#8FC49B` / `#1E3B25` | Плашка верного ответа и завершённых умений |
| `warning` / `warningContainer` | `#C87A19` / `#FDF0D5` | `#F2B45C` / `#3D2B12` | Ответ засчитан с опечаткой в диакритике, вечерний стрик под угрозой |

---

## 2. Компоненты и паттерны M3 (`Controls.kt`)

1. **`GroupedListCard` и `GroupedListRow`**:
   - Списки на главной («Чтение», «Чат», «Слово дня») и в профиле/настройках оформляются единым скруглённым контейнером (`24.dp`) с тонкими внутренними разделителями `outlineVariant` (`0.75.dp`), иконкой в тональном бейдже (`40.dp`), заголовком, подзаголовком и шевроном `›`.
2. **Нижняя панель навигации (`BottomBar.kt`)**:
   - Построена по канонам M3 `NavigationBar`: контейнер `surfaceContainer`, высота `80.dp`, овальная пилюля-индикатор `primaryContainer` (`64×32.dp`) вокруг иконки активного раздела, 3 пункта верхнего уровня (**Сегодня**, **Прогресс**, **Профиль**).
3. **Обратная связь в уроке (`FeedbackCard` и `SuccessFeedbackCard`)**:
   - Не блокирует экран модальным окном: плавно раскрывается под заданием, сохраняя видимым контекст вопроса и ответа пользователя.

---

## 3. Система поз маскота-бобра (`Mascot.kt`)

Все 15 поз бобра вырезаны из авторских спрайт-листов в `app/src/main/res/drawable-nodpi/mascot_*.png` с чистым альфа-каналом и доступны через `enum class MascotPose`:

| `MascotPose` | Ресурс | Где используется |
|---|---|---|
| `WAVE` | `R.drawable.mascot_wave` | Приветствие на главном экране («Сегодня»), когда план на день ещё впереди |
| `READING` | `R.drawable.mascot_reading` | В процессе прохождения блока на главной, экран чтения |
| `THUMBS_UP` | `R.drawable.mascot_thumbs_up` | Верный ответ в уроке, экран прогресса |
| `MEDITATE` | `R.drawable.mascot_meditate` | Шаг `timer_practice`, треки осознанности и медитации |
| `SHRUG` | `R.drawable.mascot_shrug` | Подсказка при ошибке, пустые состояния |
| `FLAG_RO` | `R.drawable.mascot_flag_ro` | Онбординг (знакомство с курсом румынского), карточка трека `ro-a1` |
| `INSIGHT` | `R.drawable.mascot_insight` | Теоретическая карточка (`InsightStep`), Лаборатория |
| `PENCIL` | `R.drawable.mascot_pencil` | Шаг письменной рефлексии (`ReflectionStep`), треки писательства |
| `STAR` | `R.drawable.mascot_star` | Плейсмент и финальный экран онбординга |
| `CELEBRATE` | `R.drawable.mascot_celebrate` | Завершение блока без ошибок (`BlockReviewScreen`), выполненный план дня |
| `THINKING` | `R.drawable.mascot_thinking` | Завершение блока с разбором ошибок, вечернее напоминание о стрике |
| `LANTERN` | `R.drawable.mascot_lantern` | Карточка и виджет «Слово дня» |
| `SCHOLAR` | `R.drawable.mascot_scholar` | Культурно-философские треки (`TrackKind.CULTURE`) |
| `SLEEPING` | `R.drawable.mascot_sleeping` | Офлайн/спящие состояния («Чат не настроен») |
| `AVATAR` | `R.drawable.mascot_avatar` | Круглый аватар в профиле и репликах собеседника в чате |

Компонент `Mascot(pose: MascotPose, size: Dp, modifier: Modifier)` рендерит PNG-ассет с `ContentScale.Fit`. `MascotPlaceholder` сохранён как обёртка над `Mascot` для обратной совместимости.

---

## 4. Виджеты домашнего экрана в стиле Duolingo (`Widgets.kt`)

Оба Glance-виджета используют выразительную композицию с крупным бобром-маскотом, динамической сменой настроения/цвета и мгновенным откликом (`BebringoWidgets.updateAll(context)` вызывается сразу после закрытия блока).

### 4.1 Виджет стрика и статуса дня (`StreakWidget`, 2×1 / 3×2)
Виджет автоматически меняет палитру, позу бобра и призыв к действию в зависимости от состояния стрика и часа суток (`LocalTime.now().hour`):
1. **План дня выполнен (`closedToday == true`)**:
   - Изумрудный/тёплый фон победы (`#1E3F29`), бобёр `mascot_celebrate`, крупный бейдж `🔥 N дн.`, статус «План на сегодня выполнен! Горжусь тобой».
2. **Блок начат, но не закрыт (`inFlight != null`)**:
   - Тёплый янтарный фон (`#FFF3DC`), бобёр `mascot_reading`, прогресс «Шаг X из 5», кнопка «Продолжить».
3. **Вечер, стрик под угрозой (`hour >= 20 && !closedToday`)**:
   - Выразительный вечерний терракотово-янтарный фон (`#3A2018`), взволнованный бобёр `mascot_thinking`, бейдж `⚠️ Стрик ждёт!`, подпись «Загляни на 3 минуты перед сном».
4. **Обычное дневное ожидание**:
   - Фирменный тёплый кремовый фон (`#FDF6EC`), приветливый бобёр `mascot_wave`, бейдж `🔥 N дн.`, кнопка «Начать блок · 3 мин».

### 4.2 Виджет «Слово дня» (`WordOfDayWidget`, 2×2)
- Выбирает слово дня детерминированно по `LocalDate.now().toEpochDay()` из словаря курса.
- Отображает бобра с фонариком (`mascot_lantern`), бейдж «СЛОВО ДНЯ», крупное слово на изучаемом языке и пример употребления.
- **Интерактивное раскрытие перевода**: нажатие на плашку перевода вызывает `ToggleWordRevealAction` (`ActionCallback`), который переключает флаг `Preferences[KEY_REVEALED]` в `GlanceStateDefinition` и мгновенно показывает/скрывает русский перевод прямо на домашнем экране без запуска Activity. Нажатие на заголовок открывает экран «Слово дня» в приложении с озвучкой.
