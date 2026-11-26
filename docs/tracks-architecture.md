# Архитектура модульного движка треков (Bebringo Track Engine v2)

**Статус:** актуальная спецификация (v2)
**Связанные файлы:**
- [`Course.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/domain/model/Course.kt)
- [`Step.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/domain/model/Step.kt)
- [`CourseFile.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/data/content/CourseFile.kt)
- [`StepMapper.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/data/content/StepMapper.kt)
- [`CourseLoader.kt`](file:///var/home/dueattendant149/bebringo/app/src/main/java/org/dueattendant149/bebringo/data/content/CourseLoader.kt)
- [`.gemini/skills/bebringo-track-author/SKILL.md`](file:///var/home/dueattendant149/bebringo/.gemini/skills/bebringo-track-author/SKILL.md)

---

## 1. Назначение и философия

Bebringo спроектирован как универсальный модульный движок микрообучения и ежедневных практик. Хотя первым встроенным треком является **Румынский язык (A1)** (`ro_a1`), архитектура ядра (`TrackMeta` → `Unit` → `Chapter` → `Scene` → `Block` → `Step` + `Lemma` + `FSRS`) не привязана жёстко к одной языковой паре и поддерживает 4 класса треков (`TrackKind`):

1. **`LANGUAGE` (Языковые треки)** — изучение иностранных языков (румынский, испанский, сербский, японский и т.д.) с поддержкой TTS (`ttsLocale`), полосы диакритики (`specialChars`), FSRS-повторений рецептивных/продуктивных карточек, чтения с адаптированной подпоркой и ролевых ИИ-сценариев.
2. **`MINDFULNESS` (Осознанность и медитация)** — ежедневные практики дыхания, сканирования тела, управления вниманием и светской майндфулнесс со встроенным таймером (`timer`), теорией (`insight`) и дневником самонаблюдения (`reflection`).
3. **`CULTURE` (Культура и философия)** — введение в буддийскую культуру, историю идей, этику и концептуальные системы (термины, символы, первоисточники, проверка понимания и рефлексия).
4. **`WRITING` (Писательское мастерство и стиль)** — практические навыки сторителлинга, редактуры, риторики и структуры текста (редактирование фраз в `transform`, разбор приёмов в `insight`, свободные мини-эссе в `reflection`).

---

## 2. Доменная модель трека (`Course.kt`)

### 2.1 `TrackMeta` и `TrackCapabilities`

Каждый YAML-файл в `assets/course/*.yml` (а также пользовательские треки в `filesDir/tracks/*.yml`) описывает один трек и загружается через `CourseLoader.loadAllTracks()`.

```kotlin
enum class TrackKind(val labelRu: String) {
    LANGUAGE("Язык"),
    MINDFULNESS("Практика"),
    CULTURE("Культура"),
    WRITING("Письмо");
}

data class TrackCapabilities(
    val tts: Boolean = true,
    val diacritics: Boolean = true,
    val fsrs: Boolean = true,
    val llmCoach: Boolean = false,
)

data class TrackMeta(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val kind: TrackKind = TrackKind.LANGUAGE,
    val mascotPose: String = "flag_ro",
    val ttsLocale: String? = "ro-RO",
    val specialChars: List<String> = listOf("ă", "â", "î", "ș", "ț"),
    val capabilities: TrackCapabilities = TrackCapabilities(),
)
```

### 2.2 Как `TrackMeta` и `TrackCapabilities` управляют UI
- `capabilities.diacritics` / `specialChars`: включает панель специальных символов (`DiacriticsBar`) на шагах письменного ввода (`typein`, `dictation`, `transform`, `reflection`). Для румынского — `ă â î ș ț`, для других языков задаётся в YAML (`specialChars`), для неязыковых треков может отключаться.
- `capabilities.tts` / `ttsLocale`: включает кнопки прослушивания произношения на шагах и в «Слове дня».
- `capabilities.fsrs`: управляет генерацией и подмешиванием карточек интервального повторения FSRS (`DefaultBlockBuilder`).
- `capabilities.llmCoach`: управляет доступностью ИИ-разбора в чате и свободной рефлексии.

---

## 3. Типы шагов (`Step.kt` и `StepMapper.kt`)

В `Step` поддерживаются **13 типов шагов**, разделённых по педагогической роли (`isProductive`, `isAudio`):

| YAML `type` | Класс в Kotlin | `isProductive` | `isAudio` | Назначение |
|---|---|---|---|---|
| `insight` | `InsightStep` | `false` | `false` | Теория до практики: правило, культурный контекст или техника на 20–40 секунд чтения с примерами (`examples: List<InsightExample>`) и советом бобра (`tip`). |
| `recognition` | `RecognitionStep` | `false` | `false` | Узнавание слова, термина или концепта: выбор 1 из 4 вариантов (`optionsRu`) + кнопка аудио и подсказка контекста. |
| `listening` | `ListeningStep` | `false` | `true` | Восприятие на слух (`audioText`) с выбором смысла из 4 вариантов (`optionsRu`). |
| `image` | `ImageStep` | `false` | `false` | Сопоставление визуального образа (`imageKey`) с термином из 4 вариантов (`optionsRo`). |
| `assemble` | `AssembleStep` | `true` | `false` | Сборка фразы или логической цепочки из банка плиток (`tokens`). В `tokens` обязательно включаются 2–3 слова-дистрактора; в уроке плитки перемешиваются детерминированно (`shuffled(Random(id.hashCode()))`). |
| `typein` | `TypeInStep` | `true` | `false` | Свободный ввод ответа с клавиатуры (`promptRu` → `answerRo`). Поддерживает список альтернативных правильных ответов (`acceptableAnswers`). |
| `dictation` | `DictationStep` | `true` | `true` | Запись услышанной фразы на слух (`audioText` → `answerRo`). |
| `transform` | `TransformStep` | `true` | `false` | Грамматическая трансформация или редакторская правка текста (`sourceRo` + `instructionRu` → `answerRo` / `acceptableAnswers`). |
| `weave` | `WeaveStep` | `false` | `false` | Контекстное вплетение термина в русскоязычный абзац (`paragraphs: List<List<TextSpan>>`). |
| `sprint` | `SprintStep` | `false` | `false` | Чтение текста на скорость и целостное восприятие (`paragraphs`, `timeLimitSec`). |
| `dialogue` | `DialogueStep` | `false` | `true` | Мини-диалог или кейс (`lines: List<DialogueLine>`) с проверочным вопросом (`questionRu`) и 4 вариантами ответа (`optionsRu`). |
| `timer` | `TimerPracticeStep` | `false` | `false` | Интерактивный таймер практики (`durationSec`, `instruction`, `cues: List<TimerCue>`) с круговым индикатором, паузой и пошаговыми подсказками по времени. |
| `reflection` | `ReflectionStep` | `true` | `false` | Свободная письменная рефлексия или мини-эссе с проверочным чек-листом самооценки (`checklist`) и минимальным объёмом ответа (`minWords`). |

---

## 4. Инварианты блока и FSRS-связывание

### 4.1 Инварианты `DefaultBlockBuilder`
Каждый блок (`Block`) обязан проходить автоматическую валидацию в `DefaultBlockBuilder.validate(block)` (проверяется в `CourseContentTest` при каждой сборке):
1. **Ровно 5 шагов** (`REQUIRED_STEPS = 5`).
2. **Минимум 1 продуктивный шаг** (`step.isProductive == true`).
3. **Минимум 1 аудиошаг** (`step.isAudio == true`).
4. **Одна оцениваемая лемма на блок и на сцену**: урок — это сцена из 2 блоков, и одна и та же лемма не должна встречаться в двух оцениваемых шагах урока. Это проверяет `CourseContentTest`.

### 4.2 Привязка шагов к леммам/концептам (`lemmaId`) и FSRS
- Каждый шаг в YAML явно указывает `lemma: <id>`. Если поле опущено, `LessonViewModel` ищет совпадение по нормализованному тексту слова/ответа среди `course.lemmas`.
- При ответе на **оцениваемый** шаг **с первой попытки** (`LessonViewModel.recordAnswer()`):
  - Записывается событие в локальную телеметрию (`TelemetryEventEntity`).
  - Карточка FSRS (`RECEPTIVE` для рецептивных шагов, `PRODUCTIVE` для продуктивных) обновляется через `FsrsScheduler.review()` с оценкой `Rating.GOOD` (или `Rating.AGAIN` при ошибке).
  - Теория (`insight`), таймер, вплетение и пробег не считаются ответом (`Step.isAssessed == false`) и FSRS не обновляют.
- Новый урок **не** подмешивает повторения в авторские шаги. `DefaultBlockBuilder.buildReviewBlock` собирает отдельную сессию «Повторение» на главной: до 10 созревших карточек, по одной на лемму, с `RecognitionStep` или `TypeInStep`.

---

## 5. Переключение треков и автообнаружение

- `CourseLoader.loadAllTracks()` автоматически обнаруживает все файлы `assets/course/*.yml` и `filesDir/tracks/*.yml` и объединяет их в единый `LoadedCourse` со списком `tracks: List<TrackMeta>`.
- Если в приложении больше одного трека, на главном экране (`HomeScreen`) отображается ряд `FilterChip` с выбором активного трека (хранится в `SettingsStore.activeTrackId`).
- Для генерации новых треков любой ИИ-моделью используется скилл [`.gemini/skills/bebringo-track-author/SKILL.md`](file:///var/home/dueattendant149/bebringo/.gemini/skills/bebringo-track-author/SKILL.md) и шаблоны из [`references/track-templates.md`](file:///var/home/dueattendant149/bebringo/.gemini/skills/bebringo-track-author/references/track-templates.md).
