package org.dueattendant149.bebringo.data.sample

import org.dueattendant149.bebringo.domain.model.AssembleStep
import org.dueattendant149.bebringo.domain.model.Block
import org.dueattendant149.bebringo.domain.model.ChatMessage
import org.dueattendant149.bebringo.domain.model.ChatScenario
import org.dueattendant149.bebringo.domain.model.DialogueLine
import org.dueattendant149.bebringo.domain.model.DialogueStep
import org.dueattendant149.bebringo.domain.model.DictationStep
import org.dueattendant149.bebringo.domain.model.ListeningStep
import org.dueattendant149.bebringo.domain.model.MistakeNote
import org.dueattendant149.bebringo.domain.model.Reading
import org.dueattendant149.bebringo.domain.model.ReadingParagraph
import org.dueattendant149.bebringo.domain.model.ReadingSource
import org.dueattendant149.bebringo.domain.model.RecognitionStep
import org.dueattendant149.bebringo.domain.model.Skill
import org.dueattendant149.bebringo.domain.model.SprintStep
import org.dueattendant149.bebringo.domain.model.Step
import org.dueattendant149.bebringo.domain.model.TextSpan
import org.dueattendant149.bebringo.domain.model.TransformStep
import org.dueattendant149.bebringo.domain.model.TypeInStep
import org.dueattendant149.bebringo.domain.model.WeaveStep
import org.dueattendant149.bebringo.domain.model.ErrorType

/**
 * Временные данные для фазы 1: экраны должны выглядеть живыми, пока нет курса.
 * Всё отсюда исчезнет, когда появится загрузчик контента.
 */
object SampleData {

    val blockTitle = "La gară"
    val chapterTitle = "Prima săptămână"
    val streakDays = 15
    val chapterDone = 3
    val chapterCurrent = 3
    val chapterTotal = 6

    val lessonSteps: List<Step> = listOf(
        AssembleStep(
            id = "s1",
            promptRu = "Я хочу купить билет на поезд.",
            tokens = listOf("bilet", "un", "vreau", "să", "cumpăr", "tren"),
            answerRo = "Vreau să cumpăr un bilet de tren.",
        ),
        TypeInStep(
            id = "s2",
            promptRu = "Доброе утро!",
            answerRo = "Bună dimineața!",
            hint = "Bună + dimineața",
        ),
        ListeningStep(
            id = "s3",
            audioText = "Trenul pleacă la ora opt.",
            translationRu = "Поезд отправляется в восемь.",
            optionsRu = listOf("Поезд отправляется в восемь.", "Поезд уже ушёл.", "Билет стоит восемь лей."),
        ),
        RecognitionStep(
            id = "s4",
            wordRo = "bilet",
            translationRu = "билет",
            optionsRu = listOf("билет", "поезд", "вокзал", "касса"),
        ),
        DictationStep(
            id = "s5",
            audioText = "La gară",
            answerRo = "La gară",
        ),
    )

    val block = Block(
        id = "block-3",
        titleRo = blockTitle,
        steps = lessonSteps,
    )

    /** Блок для экрана «Вплетение» и «Пробег». */
    val weaveStep = WeaveStep(
        id = "weave-1",
        paragraphs = listOf(
            listOf(
                TextSpan("Dimineața, Grigore se trezea întotdeauna cu "),
                TextSpan("объявлением", isRussian = true),
                TextSpan(" în ureche. "),
                TextSpan("Companionul", lemmaId = "companion"),
                TextSpan(" plutea la un metru deasupra patului și "),
                TextSpan("vindea", lemmaId = "vinde"),
                TextSpan(" ceva."),
            ),
            listOf(
                TextSpan("— Ai nevoie de un "),
                TextSpan("abonament", isRussian = true),
                TextSpan(", spunea el. — Toți au nevoie."),
            ),
        ),
    )

    val sprintStep = SprintStep(
        id = "sprint-1",
        paragraphs = listOf(
            "Grigore a deschis ușa și a ieșit în stradă. Era frig, dar cerul era senin.",
            "Omul de la colț vindea cafea. Grigore a cumpărat o cafea și a mers mai departe.",
        ),
        timeLimitSec = 60,
    )

    val dialogueStep = DialogueStep(
        id = "dialogue-1",
        title = "La gară",
        lines = listOf(
            DialogueLine("Casiera", "Bună ziua! Cu ce vă pot ajuta?"),
            DialogueLine("Grigore", "Aș vrea un bilet la Cluj, vă rog."),
            DialogueLine("Casiera", "Simplu sau dus-întors?"),
            DialogueLine("Grigore", "Simplu, mulțumesc."),
        ),
        questionRu = "Что хочет купить Григоре?",
        optionsRu = listOf("Билет до Клужа", "Кофе", "Газету", "Ничего"),
    )

    val transformStep = TransformStep(
        id = "transform-1",
        sourceRo = "Cățelul latră.",
        instructionRu = "Поставьте во множественное число",
        answerRo = "Cățeii latră.",
    )

    val readings: List<Reading> = listOf(
        Reading(
            id = "o-zi-in-cluj",
            titleRo = "O zi în Cluj",
            author = "Bebringo",
            source = ReadingSource.ORIGINAL,
            paragraphs = listOf(
                ReadingParagraph(
                    textRo = "Bună! Mă numesc Grigore. Sunt din Cluj și locuiesc într-o casă mică. În casă sunt mama, tata, sora mea și fratele meu. Casa are o cameră mare și o bucătărie. În bucătărie este masa și un scaun.",
                    translationRu = "Привет! Меня зовут Григоре. Я из Клужа и живу в маленьком доме. В доме мама, папа, моя сестра и мой брат. В доме есть большая комната и кухня. На кухне стол и стул.",
                ),
                ReadingParagraph(
                    textRo = "Dimineața beau o cafea și mănânc pâine. La ora opt merg la muncă. Lucrez într-un birou cu un coleg. La ora zece am o ședință. Seara mă întorc acasă și vorbesc cu familia. Îmi place orașul Cluj.",
                    translationRu = "Утром я пью кофе и ем хлеб. В восемь часов иду на работу. Я работаю в офисе с коллегой. В десять у меня совещание. Вечером я возвращаюсь домой и говорю с семьёй. Мне нравится город Клуж.",
                ),
            ),
        ),
        Reading(
            id = "chiriac",
            titleRo = "Tăcerea lui Grigore Pană",
            author = "Gabriel Chiriac",
            source = ReadingSource.HELION,
            paragraphs = listOf(
                ReadingParagraph(
                    textRo = "Dimineața, Grigore se trezea întotdeauna cu un Companion care plutea la un metru deasupra patului. Acesta îi spunea ce are nevoie înainte ca el să știe.",
                    translationRu = "Утром Григоре всегда просыпался с Компаньоном, который парил в метре над кроватью. Тот говорил ему, что ему нужно, раньше, чем он сам это понимал.",
                    wovenRu = mapOf("Companion" to "компаньон"),
                ),
                ReadingParagraph(
                    textRo = "Trecuseră patruzeci și trei de ani de când sistema funcționa. Nimeni nu-și mai amintea cum era fără ea.",
                    translationRu = "Прошло сорок три года с тех пор, как система работала. Никто уже не помнил, как было без неё.",
                ),
            ),
        ),
        Reading(
            id = "pascal",
            titleRo = "Mama",
            author = "Cristina Dumitru Pascal",
            source = ReadingSource.HELION,
            paragraphs = listOf(
                ReadingParagraph(
                    textRo = "Mama spăla vasele și vorbea singură. Nu era tristă, doar obosită.",
                    translationRu = "Мама мыла посуду и говорила сама с собой. Она не была грустной, просто уставшей.",
                ),
            ),
        ),
        Reading(
            id = "urmuz-furtuna",
            titleRo = "După furtună",
            author = "Urmuz",
            source = ReadingSource.PUBLIC_DOMAIN,
            paragraphs = listOf(
                ReadingParagraph(
                    textRo = "După furtună, cerul rămase senin. Oamenii ieșiră din case și priviră spre zare.",
                    translationRu = "После бури небо осталось ясным. Люди вышли из домов и посмотрели вдаль.",
                ),
            ),
        ),
        Reading(
            id = "urmuz-palnia",
            titleRo = "Pâlnia și Stamate",
            author = "Urmuz",
            source = ReadingSource.PUBLIC_DOMAIN,
            paragraphs = listOf(
                ReadingParagraph(
                    textRo = "Un apartament bine aerisit, compus din trei încăperi principale, având terasă cu geamlâc și sonerie.",
                    translationRu = "Хорошо проветриваемая квартира из трёх основных комнат, с террасой со стеклянной галереей и звонком.",
                ),
            ),
        ),
        Reading(
            id = "blecher",
            titleRo = "Berck, orașul damnaților",
            author = "Max Blecher",
            source = ReadingSource.PUBLIC_DOMAIN,
            paragraphs = listOf(
                ReadingParagraph(
                    textRo = "Orașul acesta este plin de oameni care își așteaptă vindecarea. Eu sunt unul dintre ei.",
                    translationRu = "Этот город полон людей, которые ждут своего выздоровления. Я один из них.",
                ),
            ),
        ),
    )

    val chatScenarios = listOf(
        ChatScenario(
            id = "la-gara",
            titleRo = "La gară",
            titleRu = "На вокзале",
            requiredLemmas = listOf("bilet", "tren", "gară"),
            blocksTotal = 5,
            blocksPassed = 2,
        ),
        ChatScenario(
            id = "la-cafenea",
            titleRo = "La cafenea",
            titleRu = "В кафе",
            requiredLemmas = listOf("cafea", "ceai", "nota"),
            blocksTotal = 5,
            blocksPassed = 1,
        ),
        ChatScenario(
            id = "la-piata",
            titleRo = "La piață",
            titleRu = "На рынке",
            requiredLemmas = listOf("cât costă", "apă", "lei"),
            blocksTotal = 5,
            blocksPassed = 0,
        ),
    )

    val chatScenario: ChatScenario = chatScenarios.first()

    val chatMessages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "m1",
            text = "Bună ziua! Cu ce vă pot ajuta?",
            fromUser = false,
            timeLabel = "21:14",
        ),
        ChatMessage(
            id = "m2",
            text = "Aș vrea un bilet la Cluj, vă rog.",
            fromUser = true,
            timeLabel = "21:15",
        ),
        ChatMessage(
            id = "m3",
            text = "Cu plăcere. Ai întrebat ceva?",
            fromUser = false,
            timeLabel = "21:16",
        ),
    )

    val skills: List<Skill> = listOf(
        Skill("s1", "Представиться и рассказать о себе", true),
        Skill("s2", "Заказать в кафе и оплатить", true),
        Skill("s3", "Спросить дорогу и понять ответ", true),
        Skill("s4", "Купить билет на поезд", true),
        Skill("s5", "Объяснить, что случилось вчера", false),
        Skill("s6", "Позвонить и оставить сообщение", false),
        Skill("s7", "Поговорить о работе", false),
        Skill("s8", "Записаться к врачу", false),
    )

    val reviewMistakes: List<MistakeNote> = listOf(
        MistakeNote(
            prompt = "Vreau să cumpăr un bilet de tren.",
            correct = "Пропущено «să»",
            explanationRu = "После «vreau» нужен сослагательный оборот с «să», а не инфинитив.",
            errorType = ErrorType.WRONG_FORM,
        ),
        MistakeNote(
            prompt = "Bună dimineața!",
            correct = "Bună dimineața",
            explanationRu = "Диакритика: «ца» пишется как «ța», а не «ta».",
            errorType = ErrorType.DIACRITICS,
        ),
        MistakeNote(
            prompt = "La gară",
            correct = "la gară",
            explanationRu = "Артикль не нужен: «la gară» — это «на вокзал», без определённого артикля.",
            errorType = ErrorType.WRONG_WORD,
        ),
    )

    /** Слово дня для главного экрана и виджета. */
    val wordOfDay = "tren"
    val wordOfDayTranslation = "поезд"
}
