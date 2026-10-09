package com.example.myapplication.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Word
import com.example.myapplication.data.repository.AppRepository
import com.example.myapplication.util.TextToSpeechHelper

data class Question(
    val word: Word,
    val options: List<String>,
    val correctAnswer: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestScreen(
    repository: AppRepository,
    folderId: String,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val ttsHelper = remember { TextToSpeechHelper(context) }
    DisposableEffect(Unit) {
        onDispose { ttsHelper.shutdown() }
    }

    val folder = remember { repository.getFolderById(folderId) }
    val words = remember { repository.getWordsByFolder(folderId) }

    var questions by remember { mutableStateOf<List<Question>>(emptyList()) }
    var currentQuestionIndex by remember { mutableIntStateOf(0) }
    var selectedOption by remember { mutableStateOf<String?>(null) }
    var isAnswered by remember { mutableStateOf(false) }
    var correctCount by remember { mutableIntStateOf(0) }
    var isFinished by remember { mutableStateOf(false) }

    fun generateQuestions() {
        if (words.size < 2) return

        val generated = words.shuffled().map { targetWord ->
            val correct = targetWord.kazakh
            val otherKazakh = words.filter { it.id != targetWord.id }.map { it.kazakh }.distinct().shuffled()
            val wrongOptions = otherKazakh.take(3)
            val allOptions = (wrongOptions + correct).shuffled()

            Question(
                word = targetWord,
                options = allOptions,
                correctAnswer = correct
            )
        }
        questions = generated
        currentQuestionIndex = 0
        selectedOption = null
        isAnswered = false
        correctCount = 0
        isFinished = false
    }

    LaunchedEffect(folderId) {
        generateQuestions()
    }

    if (folder == null || words.size < 2) {
        Scaffold { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Тест тапсыру үшін папкада кемінде 2 сөз болуы керек!",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    val currentQuestion = if (currentQuestionIndex < questions.size) questions[currentQuestionIndex] else null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Тест: ${folder.name}") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Артқа")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (isFinished || currentQuestion == null) {
                // Results Screen
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    val percent = if (questions.isNotEmpty()) (correctCount * 100) / questions.size else 0

                    val (titleText, messageText, titleColor) = when {
                        percent == 100 -> Triple("Ғажап нәтиже! 🌟 100%", "Барлық сұраққа мүлтіксіз дұрыс жауап бердіңіз!", Color(0xFF059669))
                        percent >= 70 -> Triple("Өте жақсы! 🏆 $percent%", "Жақсы білім деңгейі! Осылай жалғастырыңыз!", MaterialTheme.colorScheme.primary)
                        percent >= 50 -> Triple("Жақсы талпыныс! 👍 $percent%", "Тағы бір рет қайталасаңыз одан да жақсы болады!", MaterialTheme.colorScheme.tertiary)
                        else -> Triple("Ештеңе етпейді! 💪 $percent%", "'Заучивание' режимінде сөздерді қайталап, қайта тапсырыңыз!", MaterialTheme.colorScheme.error)
                    }

                    Text(
                        text = titleText,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = titleColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Сіз ${questions.size} сұрақтың $correctCount-сіне дұрыс жауап бердіңіз.\n$messageText",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { generateQuestions() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Тестті қайта тапсыру", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Active Question View
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Сұрақ ${currentQuestionIndex + 1} / ${questions.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(
                        text = "Дұрыс: $correctCount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (currentQuestionIndex + 1).toFloat() / questions.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Question Box
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Аудармасын табыңыз:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = currentQuestion.word.english,
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            IconButton(
                                onClick = { ttsHelper.speak(currentQuestion.word.english) },
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                    contentDescription = "Дыбыстау",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // 4 Options
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    currentQuestion.options.forEach { option ->
                        val isCorrect = option == currentQuestion.correctAnswer
                        val isSelected = option == selectedOption

                        val backgroundColor = when {
                            !isAnswered -> MaterialTheme.colorScheme.surface
                            isCorrect -> Color(0xFFD1FAE5) // Light green
                            isSelected -> Color(0xFFFEE2E2) // Light red
                            else -> MaterialTheme.colorScheme.surface
                        }

                        val borderColor = when {
                            !isAnswered && isSelected -> MaterialTheme.colorScheme.primary
                            !isAnswered -> MaterialTheme.colorScheme.outline
                            isCorrect -> Color(0xFF10B981)
                            isSelected -> Color(0xFFEF4444)
                            else -> MaterialTheme.colorScheme.outline
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.5.dp, borderColor, RoundedCornerShape(12.dp))
                                .clickable(enabled = !isAnswered) {
                                    selectedOption = option
                                    isAnswered = true
                                    if (isCorrect) correctCount++
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = backgroundColor)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text(
                                    text = option,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                // Next question button
                if (isAnswered) {
                    Button(
                        onClick = {
                            if (currentQuestionIndex + 1 < questions.size) {
                                currentQuestionIndex++
                                selectedOption = null
                                isAnswered = false
                            } else {
                                isFinished = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (currentQuestionIndex + 1 < questions.size) "Келесі сұрақ ➔" else "Нәтижені көру 🏆",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
