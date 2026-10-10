package com.example.myapplication.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.model.Folder
import com.example.myapplication.data.repository.AppRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    repository: AppRepository,
    onFolderClick: (String) -> Unit,
    onProfileClick: () -> Unit
) {
    val currentUser = remember { repository.getCurrentUser() }
    var folders by remember { mutableStateOf(repository.getFolders()) }
    var searchQuery by remember { mutableStateOf("") }
    var showAddDialog by remember { mutableStateOf(false) }
    var showQuickTranslateDialog by remember { mutableStateOf(false) }

    fun refreshFolders() {
        folders = repository.getFolders()
    }

    val filteredFolders = folders.filter {
        it.name.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Сәлем, ${currentUser?.name ?: "Пайдаланушы"}! 👋",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Папкалар мен сөздіктер",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Quick Translator Icon Button
                    IconButton(
                        onClick = { showQuickTranslateDialog = true },
                        modifier = Modifier
                            .padding(end = 4.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = "Аудармашы",
                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }

                    // Daily Streak Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(text = "🔥", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${currentUser?.currentStreak ?: 1} күн",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    IconButton(
                        onClick = onProfileClick,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Профиль",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = "Папка қосу") },
                text = { Text("Жаңа папка") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            if (folders.isNotEmpty()) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Папканы іздеу...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (folders.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Папкалар әлі жоқ",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Төмендегі батырманы басып алғашқы папкаңызды жасаңыз",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else if (filteredFolders.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "'$searchQuery' бойынша папка табылмады 🔍",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredFolders, key = { it.id }) { folder ->
                            val words = repository.getWordsByFolder(folder.id)
                            val learnedCount = words.count { it.isLearned }

                            FolderItemCard(
                                folder = folder,
                                totalWords = words.size,
                                learnedWords = learnedCount,
                                onClick = { onFolderClick(folder.id) },
                                onDelete = {
                                    repository.deleteFolder(folder.id)
                                    refreshFolders()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddFolderDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, desc ->
                repository.addFolder(name, desc)
                refreshFolders()
                showAddDialog = false
            }
        )
    }

    if (showQuickTranslateDialog) {
        QuickTranslateDialog(
            folders = folders,
            onDismiss = { showQuickTranslateDialog = false },
            onSaveToFolder = { selectedFolderId, eng, kaz ->
                repository.addWord(selectedFolderId, eng, kaz)
                refreshFolders()
                showQuickTranslateDialog = false
            }
        )
    }
}

@Composable
fun QuickTranslateDialog(
    folders: List<Folder>,
    onDismiss: () -> Unit,
    onSaveToFolder: (String, String, String) -> Unit
) {
    var englishWord by remember { mutableStateOf("") }
    var kazakhWord by remember { mutableStateOf("") }
    var selectedFolderId by remember { mutableStateOf(folders.firstOrNull()?.id ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "🌐 Жылдам Аудармашы") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = englishWord,
                    onValueChange = { englishWord = it },
                    label = { Text("Ағылшынша сөз") },
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

                if (folders.isNotEmpty()) {
                    Text(
                        text = "Сақталатын папканы таңдаңыз:",
                        style = MaterialTheme.typography.labelMedium
                    )
                    folders.forEach { folder ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedFolderId = folder.id }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedFolderId == folder.id,
                                onClick = { selectedFolderId = folder.id }
                            )
                            Text(text = folder.name, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (englishWord.isNotBlank() && kazakhWord.isNotBlank() && selectedFolderId.isNotBlank()) {
                        onSaveToFolder(selectedFolderId, englishWord.trim(), kazakhWord.trim())
                    }
                },
                enabled = englishWord.isNotBlank() && kazakhWord.isNotBlank() && selectedFolderId.isNotBlank()
            ) {
                Text("Папкаға сақтау 📁")
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
fun FolderItemCard(
    folder: Folder,
    totalWords: Int,
    learnedWords: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (folder.description.isNotBlank()) {
                    Text(
                        text = folder.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "$totalWords сөз • $learnedWords жатталды",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Жою",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AddFolderDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var folderDesc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Жаңа папка құру 📁") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Папка атауы (мысалы: Тамақ өнімдері)") },
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = folderDesc,
                    onValueChange = { folderDesc = it },
                    label = { Text("Сипаттамасы (міндетті емес)") },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onConfirm(folderName.trim(), folderDesc.trim())
                    }
                },
                enabled = folderName.isNotBlank()
            ) {
                Text("Құру")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Бас тарту")
            }
        }
    )
}
