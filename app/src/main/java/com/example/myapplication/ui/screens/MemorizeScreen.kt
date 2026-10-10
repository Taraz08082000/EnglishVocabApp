package com.example.myapplication.ui.screens

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.repository.AppRepository
import com.example.myapplication.util.TextToSpeechHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizeScreen(
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
    val initialWords = remember { repository.getWordsByFolder(folderId) }
    var currentIndex by remember { mutableIntStateOf(0) }
    var isFlipped by remember { mutableStateOf(false) }
    var isFinished by remember { mutableStateOf(false) }

    var speechFeedback by remember { mutableStateOf<String?>(null) }
    var speechResultColor by remember { mutableStateOf(Color.Gray) }

    if (folder == null || initialWords.isEmpty()) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    val currentWord = if (currentIndex < initialWords.size) initialWords[currentIndex] else null

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: ""
            if (spokenText.isNotBlank() && currentWord != null) {
                if (spokenText.trim().equals(currentWord.english.trim(), ignoreCase = true)) {
                    speechFeedback = "Өте жақсы! Дұрыс айтылды 🎯 ($spokenText)"
                    speechResultColor = Color(0xFF059669)
                } else {
                    speechFeedback = "Естілгені: '$spokenText' (Қайталап айтып көріңіз 🎙️)"
                    speechResultColor = Color(0xFFD97706)
                }
            }
        }
    }

    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "cardFlip"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Заучивание: ${folder.name}") },
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
            if (isFinished || currentWord == null) {
                // Completion State
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Жарайсың! 🎉",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Барлық карточкаларды қарап шықтыңыз!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            currentIndex = 0
                            isFlipped = false
                            isFinished = false
                            speechFeedback = null
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Қайтадан бастау")
                    }
                }
            } else {
                // Flashcard learning view
                Text(
                    text = "${currentIndex + 1} / ${initialWords.size} сөз",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (currentIndex + 1).toFloat() / initialWords.size },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Flashcard
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .graphicsLayer {
                            rotationY = rotation
                            cameraDistance = 12 * density
                        }
                        .clickable {
                            isFlipped = !isFlipped
                            speechFeedback = null
                        },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isFlipped) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (rotation <= 90f) {
                            // Front of card (English)
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Ағылшынша",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(12.dp))

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = currentWord.english,
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                    IconButton(
                                        onClick = { ttsHelper.speak(currentWord.english) },
                                        modifier = Modifier.padding(start = 2.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                                            contentDescription = "Дыбыстау",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
                                                putExtra(RecognizerIntent.EXTRA_PROMPT, "Ағылшынша сөзді айтыңыз: ${currentWord.english}")
                                            }
                                            speechLauncher.launch(intent)
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Mic,
                                            contentDescription = "Айтылуды тексеру",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                if (speechFeedback != null) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = speechFeedback!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = speechResultColor,
                                        textAlign = TextAlign.Center
                                    )
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "💡 Төңкеру үшін карточканы басыңыз",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            // Back of card (Kazakh translation + rotationY 180 flip)
                            Column(
                                modifier = Modifier.graphicsLayer { rotationY = 180f },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Қазақша аудармасы",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = currentWord.kazakh,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    textAlign = TextAlign.Center
                                )
                                if (currentWord.exampleSentence.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "“${currentWord.exampleSentence}”",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Action controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            isFlipped = false
                            speechFeedback = null
                            if (currentIndex + 1 < initialWords.size) {
                                currentIndex++
                            } else {
                                isFinished = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Қайталау керек")
                    }

                    Button(
                        onClick = {
                            repository.toggleWordLearned(currentWord.id)
                            isFlipped = false
                            speechFeedback = null
                            if (currentIndex + 1 < initialWords.size) {
                                currentIndex++
                            } else {
                                isFinished = true
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Білемін!")
                    }
                }
            }
        }
    }
}
