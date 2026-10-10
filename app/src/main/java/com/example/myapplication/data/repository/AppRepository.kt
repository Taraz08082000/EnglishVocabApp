package com.example.myapplication.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.myapplication.data.model.Folder
import com.example.myapplication.data.model.User
import com.example.myapplication.data.model.Word
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AppRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // ----------------------------------------------------------------
    // Session, User & Settings
    // ----------------------------------------------------------------

    fun getCurrentUserId(): String? {
        return prefs.getString("current_user_id", null)
    }

    fun setCurrentUserId(userId: String?) {
        prefs.edit().putString("current_user_id", userId).apply()
    }

    fun getCurrentUser(): User? {
        val currentId = getCurrentUserId() ?: return null
        return getUsers().find { it.id == currentId }
    }

    fun getUsers(): List<User> {
        val json = prefs.getString("users_list", null) ?: return emptyList()
        val type = object : TypeToken<List<User>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveUsers(users: List<User>) {
        val json = gson.toJson(users)
        prefs.edit().putString("users_list", json).apply()
    }

    fun getThemeMode(): String {
        return prefs.getString("theme_mode", "SYSTEM") ?: "SYSTEM"
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
    }

    fun registerUser(name: String, email: String, passwordHash: String): Result<User> {
        val users = getUsers().toMutableList()
        if (users.any { it.email.equals(email, ignoreCase = true) }) {
            return Result.failure(Exception("Бұл email тіркеліп қойған"))
        }
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val newUser = User(
            id = UUID.randomUUID().toString(),
            name = name,
            email = email,
            passwordHash = passwordHash,
            currentStreak = 1,
            lastActiveDate = today
        )
        users.add(newUser)
        saveUsers(users)
        setCurrentUserId(newUser.id)

        // Generate sample initial folders for new user
        createDefaultDataForUser(newUser.id)

        return Result.success(newUser)
    }

    fun loginUser(email: String, passwordHash: String): Result<User> {
        val user = getUsers().find {
            it.email.equals(email, ignoreCase = true) && it.passwordHash == passwordHash
        }
        return if (user != null) {
            setCurrentUserId(user.id)
            updateDailyStreak()
            Result.success(user)
        } else {
            Result.failure(Exception("Email немесе құпия сөз қате!"))
        }
    }

    fun logout() {
        setCurrentUserId(null)
    }

    fun updateUserNotifications(enabled: Boolean) {
        val currentUserId = getCurrentUserId() ?: return
        val users = getUsers().map { user ->
            if (user.id == currentUserId) user.copy(notificationsEnabled = enabled) else user
        }
        saveUsers(users)
    }

    fun updateDailyStreak() {
        val currentUser = getCurrentUser() ?: return
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (currentUser.lastActiveDate == today) return // Already updated today

        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(System.currentTimeMillis() - 86400000L))
        val newStreak = if (currentUser.lastActiveDate == yesterday) currentUser.currentStreak + 1 else 1

        val updatedUsers = getUsers().map { user ->
            if (user.id == currentUser.id) {
                user.copy(currentStreak = newStreak, lastActiveDate = today)
            } else user
        }
        saveUsers(updatedUsers)
    }

    // ----------------------------------------------------------------
    // Folders
    // ----------------------------------------------------------------

    fun getFolders(): List<Folder> {
        val currentUserId = getCurrentUserId() ?: return emptyList()
        val json = prefs.getString("folders_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Folder>>() {}.type
        val allFolders: List<Folder> = gson.fromJson(json, type) ?: emptyList()
        return allFolders.filter { it.userId == currentUserId }
    }

    private fun saveAllFolders(allFolders: List<Folder>) {
        val json = gson.toJson(allFolders)
        prefs.edit().putString("folders_list", json).apply()
    }

    private fun getAllFolders(): List<Folder> {
        val json = prefs.getString("folders_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Folder>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    fun addFolder(name: String, description: String = "", colorHex: String = "#4F46E5"): Folder? {
        val currentUserId = getCurrentUserId() ?: return null
        val allFolders = getAllFolders().toMutableList()
        val newFolder = Folder(
            id = UUID.randomUUID().toString(),
            userId = currentUserId,
            name = name,
            description = description,
            colorHex = colorHex
        )
        allFolders.add(newFolder)
        saveAllFolders(allFolders)
        return newFolder
    }

    fun deleteFolder(folderId: String) {
        val allFolders = getAllFolders().filterNot { it.id == folderId }
        saveAllFolders(allFolders)

        // Delete all words in this folder
        val allWords = getAllWords().filterNot { it.folderId == folderId }
        saveAllWords(allWords)
    }

    fun getFolderById(folderId: String): Folder? {
        return getAllFolders().find { it.id == folderId }
    }

    // ----------------------------------------------------------------
    // Words
    // ----------------------------------------------------------------

    private fun getAllWords(): List<Word> {
        val json = prefs.getString("words_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Word>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    private fun saveAllWords(allWords: List<Word>) {
        val json = gson.toJson(allWords)
        prefs.edit().putString("words_list", json).apply()
    }

    fun getWordsByFolder(folderId: String): List<Word> {
        return getAllWords().filter { it.folderId == folderId }
    }

    fun addWord(folderId: String, english: String, kazakh: String, phonetic: String = "", exampleSentence: String = ""): Word {
        val allWords = getAllWords().toMutableList()
        val newWord = Word(
            id = UUID.randomUUID().toString(),
            folderId = folderId,
            english = english,
            kazakh = kazakh,
            phonetic = phonetic,
            exampleSentence = exampleSentence
        )
        allWords.add(newWord)
        saveAllWords(allWords)
        return newWord
    }

    fun deleteWord(wordId: String) {
        val allWords = getAllWords().filterNot { it.id == wordId }
        saveAllWords(allWords)
    }

    fun toggleWordLearned(wordId: String) {
        val allWords = getAllWords().map { word ->
            if (word.id == wordId) {
                word.copy(isLearned = !word.isLearned)
            } else {
                word
            }
        }
        saveAllWords(allWords)
    }

    fun getRandomWordForWidget(): Word? {
        val allWords = getAllWords()
        return if (allWords.isNotEmpty()) allWords.random() else null
    }

    // ----------------------------------------------------------------
    // Initial Default Data
    // ----------------------------------------------------------------

    private fun createDefaultDataForUser(userId: String) {
        val allFolders = getAllFolders().toMutableList()
        val allWords = getAllWords().toMutableList()

        val f1 = Folder(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "Күнделікті сөздер (Daily Words)",
            description = "Жиі қолданылатын ағылшын сөздері",
            colorHex = "#2563EB"
        )
        val f2 = Folder(
            id = UUID.randomUUID().toString(),
            userId = userId,
            name = "Сайхат пен Демалыс (Travel)",
            description = "Саяхаттауға арналған сөздік",
            colorHex = "#059669"
        )

        allFolders.add(f1)
        allFolders.add(f2)

        // Sample words for f1
        allWords.add(Word(UUID.randomUUID().toString(), f1.id, "Hello", "Сәлем", "[həˈloʊ]", "Hello, how are you?"))
        allWords.add(Word(UUID.randomUUID().toString(), f1.id, "Friend", "Дос", "[frɛnd]", "She is my best friend."))
        allWords.add(Word(UUID.randomUUID().toString(), f1.id, "Success", "Табыс / Сәттілік", "[səkˈsɛs]", "I wish you success."))
        allWords.add(Word(UUID.randomUUID().toString(), f1.id, "Knowledge", "Білім", "[ˈnɑːlɪdʒ]", "Knowledge is power."))
        allWords.add(Word(UUID.randomUUID().toString(), f1.id, "Opportunity", "Мүмкіндік", "[ˌɑːpərˈtuːnəti]", "Don't miss this opportunity."))

        // Sample words for f2
        allWords.add(Word(UUID.randomUUID().toString(), f2.id, "Airport", "Әуежай", "[ˈɛər.pɔːt]", "We arrived at the airport."))
        allWords.add(Word(UUID.randomUUID().toString(), f2.id, "Hotel", "Қонақ үй", "[hoʊˈtɛl]", "The hotel was very comfortable."))
        allWords.add(Word(UUID.randomUUID().toString(), f2.id, "Ticket", "Билет", "[ˈtɪk.ɪt]", "I bought a flight ticket."))

        saveAllFolders(allFolders)
        saveAllWords(allWords)
    }
}
