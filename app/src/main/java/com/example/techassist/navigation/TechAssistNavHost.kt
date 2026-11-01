package com.example.techassist.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.navArgument
import com.example.techassist.ui.checklist.ChecklistDetailScreen
import com.example.techassist.ui.checklist.ChecklistListScreen
import com.example.techassist.ui.eval.EvaluationScreen
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.techassist.ui.home.HomeScreen
import com.example.techassist.ui.library.DocumentLibraryScreen
import com.example.techassist.ui.chat.ChatScreen
import com.example.techassist.ui.llm.LlmDebugScreen
import com.example.techassist.ui.troubleshoot.TroubleshootScreen
import com.example.techassist.ui.retrieval.RetrievalDebugScreen
import kotlinx.coroutines.flow.Flow

private object Routes {
    const val HOME = "home"
    const val LIBRARY = "library"
    const val RETRIEVAL_DEBUG = "retrieval_debug"
    const val LLM_DEBUG = "llm_debug"
    const val CHAT = "chat"
    const val TROUBLESHOOT = "troubleshoot"
    const val CHECKLISTS = "checklists"
    const val CHECKLIST_DETAIL = "checklist/{id}"
    const val EVALUATION = "evaluation"
}


@Composable
fun TechAssistNavHost(isOnlineFlow: Flow<Boolean>) {
    val navController = rememberNavController()
    val isOnline by isOnlineFlow.collectAsState(initial = false)

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                isDeviceOnline = isOnline,
                onOpenLibrary = { navController.navigate(Routes.LIBRARY) },
                onOpenRetrievalDebug = { navController.navigate(Routes.RETRIEVAL_DEBUG) },
                onOpenLlmDebug = { navController.navigate(Routes.LLM_DEBUG) },
                onOpenChat = { navController.navigate(Routes.CHAT) },
                onOpenTroubleshoot = { navController.navigate(Routes.TROUBLESHOOT) },
                onOpenChecklists = { navController.navigate(Routes.CHECKLISTS) },
                onOpenEvaluation = { navController.navigate(Routes.EVALUATION) },
            )
        }
        composable(Routes.LIBRARY) {
            DocumentLibraryScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.RETRIEVAL_DEBUG) {
            RetrievalDebugScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.EVALUATION) {
            EvaluationScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CHECKLISTS) {
            ChecklistListScreen(
                onBack = { navController.popBackStack() },
                onOpen = { id -> navController.navigate("checklist/$id") },
            )
        }
        composable(
            route = Routes.CHECKLIST_DETAIL,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) {
            ChecklistDetailScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.TROUBLESHOOT) {
            TroubleshootScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CHAT) {
            ChatScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.LLM_DEBUG) {
            LlmDebugScreen(onBack = { navController.popBackStack() })
        }
    }
}