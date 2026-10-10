package com.example.myapplication.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Word
import com.example.myapplication.data.repository.AppRepository
import com.example.myapplication.util.TextToSpeechHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    repository: AppRepository,
    folderId: String,
    onBackClick: () -> Unit,
    onStartMemorizeClick: (String) -> Unit,
    onStartTestClick: (String) -> Unit,
    onStartMatchGameClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val ttsHelper = remember { TextToSpeechHelper(context) }
    DisposableEffect(Unit) {
        onDispose { ttsHelper.shutdown() }
    }

    val folder = remember { repository.getFolderById(folderId) }
    var words by remember { mutableStateOf(repository.getWordsByFolder(folderId)) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableIntStateOf(0) } // 0: All, 1: Unlearned, 2: Learned
    var showAddWordDialog by remember { mutableStateOf(false) }
    var scannedImageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageScanDialog by remember { mutableStateOf(false) }
    var showAudioPlayerDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            scannedImageUri = uri
            showImageScanDialog = true
        }
    }

    fun refreshWords() {
        words = repository.getWordsByFolder(folderId)
    }

    if (folder == null) {
        LaunchedEffect(Unit) { onBackClick() }
        return
    }

    val learnedCount = words.count { it.isLearned }

    val filteredWords = words.filter { word ->
        val matchesSearch = word.english.contains(searchQuery, ignoreCase = true) ||
                word.kazakh.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            1 -> !word.isLearned
            2 -> word.isLearned
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Артқа"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val shareText = StringBuilder().apply {
                                appendLine("📚 Папка: ${folder.name}")
                                appendLine("------------------------")
                                words.forEachIndexed { index, w ->
                                    appendLine("${index + 1}. ${w.english} ${w.phonetic} - ${w.kazakh}")
                                }
                                appendLine("------------------------")
                                appendLine("Ағылшын-Қазақ сөздігі қосымшасынан 🚀")
                            }.toString()

                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, folder.name)
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(Intent.createChooser(intent, "Папканы бөлісу"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Папканы бөлісу",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Фотодан сөз қосу")
                }

                ExtendedFloatingActionButton(
                    onClick = { showAddWordDialog = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Жаңа сөз қосу") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Action Buttons for Learning Modes
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Прогресс: $learnedCount / ${words.size} сөз жатталды",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { if (words.isNotEmpty()) learnedCount.toFloat() / words.size else 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onStartMemorizeClick(folderId) },
                            enabled = words.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Заучивание", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onStartTestClick(folderId) },
                            enabled = words.size >= 2,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Icon(Icons.Default.Quiz, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Тест", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onStartMatchGameClick(folderId) },
                            enabled = words.size >= 3,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Icon(Icons.Default.Extension, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Ойын 🎮", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { showAudioPlayerDialog = true },
                            enabled = words.isNotEmpty(),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            contentPadding = PaddingValues(horizontal = 2.dp)
                        ) {
                            Icon(Icons.Default.Headphones, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Аудио 🎧", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Search Bar & Filter Chips
            if (words.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Сөзді іздеу...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedFilter == 0,
                        onClick = { selectedFilter = 0 },
                        label = { Text("Барлығы (${words.size})") }
                    )
                    FilterChip(
                        selected = selectedFilter == 1,
                        onClick = { selectedFilter = 1 },
                        label = { Text("Оқылуда (${words.size - learnedCount})") }
                    )
                    FilterChip(
                        selected = selectedFilter == 2,
                        onClick = { selectedFilter = 2 },
                        label = { Text("Жатталғандар ($learnedCount)") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (words.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Бұл папкада әлі сөздер жоқ.\n'Жаңа сөз қосу' немесе 'Фотодан сөз қосу' батырмасын басыңыз!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else if (filteredWords.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Сөздер табылмады 🔍",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(filteredWords, key = { it.id }) { word ->
                        WordItemCard(
                            word = word,
                            onToggleLearned = {
                                repository.toggleWordLearned(word.id)
                                refreshWords()
                            },
                            onDelete = {
                                repository.deleteWord(word.id)
                                refreshWords()
                            },
                            onSpeak = { text ->
                                ttsHelper.speak(text)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showAddWordDialog) {
        AddWordDialog(
            onDismiss = { showAddWordDialog = false },
            onConfirm = { eng, kaz, ex ->
                repository.addWord(folderId, eng, kaz, "", ex)
                refreshWords()
                showAddWordDialog = false
            }
        )
    }

    if (showImageScanDialog && scannedImageUri != null) {
        ImageScanDialog(
            imageUri = scannedImageUri!!,
            onDismiss = { showImageScanDialog = false; scannedImageUri = null },
            onConfirm = { eng, kaz, ex ->
                repository.addWord(folderId, eng, kaz, "", ex)
                refreshWords()
                showImageScanDialog = false
                scannedImageUri = null
            }
        )
    }

    if (showAudioPlayerDialog) {
        AudioPlayerDialog(
            words = words,
            ttsHelper = ttsHelper,
            onDismiss = { showAudioPlayerDialog = false }
        )
    }
}

@Composable
fun AudioPlayerDialog(
    words: List<Word>,
    ttsHelper: TextToSpeechHelper,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    var isPlaying by remember { mutableStateOf(false) }

    val currentWord = if (currentIndex < words.size) words[currentIndex] else null

    LaunchedEffect(isPlaying, currentIndex) {
        if (isPlaying && currentWord != null) {
            ttsHelper.speak(currentWord.english)
            delay(2500L)
            ttsHelper.speak(currentWord.kazakh)
            delay(2500L)
            if (currentIndex + 1 < words.size) {
                currentIndex++
            } else {
                isPlaying = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("🎧 Hands-free Авто-Ойнатқыш") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                if (currentWord != null) {
                    Text(text = "${currentIndex + 1} / ${words.size} сөз", style = MaterialTheme.typography.labelSmall)
                    Text(text = currentWord.english, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    if (currentWord.phonetic.isNotBlank()) {
                        Text(text = currentWord.phonetic, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(text = currentWord.kazakh, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (currentIndex > 0) currentIndex-- }) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Алдыңғы")
                    }
                    IconButton(
                        onClick = { isPlaying = !isPlaying },
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { if (currentIndex + 1 < words.size) currentIndex++ }) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Келесі")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Жабу")
            }
        }
    )
}

@Composable
fun WordItemCard(
    word: Word,
    onToggleLearned: () -> Unit,
    onDelete: () -> Unit,
    onSpeak: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onToggleLearned) {
                Icon(
                    imageVector = if (word.isLearned) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                    tint = if (word.isLearned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = word.english,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (word.phonetic.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = word.phonetic,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(
                        onClick = { onSpeak(word.english) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Дыбыстау",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = word.kazakh,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (word.exampleSentence.isNotBlank()) {
                    Text(
                        text = "“${word.exampleSentence}”",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Жою",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun AddWordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var englishWord by remember { mutableStateOf("") }
    var kazakhWord by remember { mutableStateOf("") }
    var exampleSentence by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Жаңа сөз қосу ✍️") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = englishWord,
                    onValueChange = { englishWord = it },
                    label = { Text("Ағылшынша (English word)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kazakhWord,
                    onValueChange = { kazakhWord = it },
                    label = { Text("Қазақша аудармасы") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = exampleSentence,
                    onValueChange = { exampleSentence = it },
                    label = { Text("Мысал сөйлем (міндетті емес)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (englishWord.isNotBlank() && kazakhWord.isNotBlank()) {
                        onConfirm(englishWord.trim(), kazakhWord.trim(), exampleSentence.trim())
                    }
                },
                enabled = englishWord.isNotBlank() && kazakhWord.isNotBlank()
            ) {
                Text("Қосу")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Бас тарту")
            }
        }
    )
}

@Composable
fun ImageScanDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onConfirm: (String, String, String) -> Unit
) {
    var englishWord by remember { mutableStateOf("") }
    var kazakhWord by remember { mutableStateOf("") }
    var exampleSentence by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "📸 Фотодан сөз сақтау") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Тандалған суреттен сөзді енгізіп, сөздікке сақтаңыз:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = englishWord,
                    onValueChange = { englishWord = it },
                    label = { Text("Ағылшын сөзі") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = kazakhWord,
                    onValueChange = { kazakhWord = it },
                    label = { Text("Қазақша аудармасы") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = exampleSentence,
                    onValueChange = { exampleSentence = it },
                    label = { Text("Мысал сөйлем") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (englishWord.isNotBlank() && kazakhWord.isNotBlank()) {
                        onConfirm(englishWord.trim(), kazakhWord.trim(), exampleSentence.trim())
                    }
                },
                enabled = englishWord.isNotBlank() && kazakhWord.isNotBlank()
            ) {
                Text("Сақтау")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Бас тарту")
            }
        }
    )
}
