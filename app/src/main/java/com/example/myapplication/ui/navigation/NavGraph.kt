package com.example.myapplication.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.data.repository.AppRepository
import com.example.myapplication.ui.screens.*

@Composable
fun AppNavGraph(repository: AppRepository) {
    val navController = rememberNavController()
    val isLoggedIn = repository.getCurrentUserId() != null
    val startDestination = if (isLoggedIn) "folders" else "register"

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("register") {
            RegisterScreen(
                repository = repository,
                onRegisterSuccess = {
                    navController.navigate("folders") {
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate("login")
                }
            )
        }

        composable("login") {
            LoginScreen(
                repository = repository,
                onLoginSuccess = {
                    navController.navigate("folders") {
                        popUpTo("login") { inclusive = true }
                        popUpTo("register") { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }

        composable("folders") {
            FoldersScreen(
                repository = repository,
                onFolderClick = { folderId ->
                    navController.navigate("folder_detail/$folderId")
                },
                onProfileClick = {
                    navController.navigate("profile")
                }
            )
        }

        composable(
            route = "folder_detail/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            FolderDetailScreen(
                repository = repository,
                folderId = folderId,
                onBackClick = { navController.popBackStack() },
                onStartMemorizeClick = { fId -> navController.navigate("memorize/$fId") },
                onStartTestClick = { fId -> navController.navigate("test/$fId") }
            )
        }

        composable(
            route = "memorize/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            MemorizeScreen(
                repository = repository,
                folderId = folderId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = "test/{folderId}",
            arguments = listOf(navArgument("folderId") { type = NavType.StringType })
        ) { backStackEntry ->
            val folderId = backStackEntry.arguments?.getString("folderId") ?: ""
            TestScreen(
                repository = repository,
                folderId = folderId,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("profile") {
            ProfileScreen(
                repository = repository,
                onBackClick = { navController.popBackStack() },
                onLogoutClick = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
