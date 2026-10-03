package com.scanflow.app.ui.navigation

import android.content.Intent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.scanflow.app.ScanFlowApplication
import com.scanflow.app.domain.model.Document
import com.scanflow.app.ui.screens.AiChatScreen
import com.scanflow.app.ui.screens.CompareScreen
import com.scanflow.app.ui.screens.DocumentViewerScreen
import com.scanflow.app.ui.screens.DocumentsScreen
import com.scanflow.app.ui.screens.HomeScreen
import com.scanflow.app.ui.screens.ScannerScreen
import com.scanflow.app.ui.screens.SettingsScreen
import com.scanflow.app.ui.screens.ToolActionScreen
import com.scanflow.app.ui.screens.ToolsScreen
import com.scanflow.app.ui.viewmodel.AiViewModel
import com.scanflow.app.ui.viewmodel.DocumentsViewModel
import com.scanflow.app.ui.viewmodel.HomeViewModel
import com.scanflow.app.ui.viewmodel.ScannerViewModel
import com.scanflow.app.ui.viewmodel.ToolActionViewModel
import com.scanflow.app.ui.viewmodel.ToolsViewModel
import com.scanflow.app.ui.viewmodel.ViewerViewModel
import java.io.File

@Composable
fun ScanFlowNavHost() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val container = remember { ScanFlowApplication.container }

    val homeViewModel: HomeViewModel = viewModel { HomeViewModel(container.documentRepository) }
    val documentsViewModel: DocumentsViewModel = viewModel { DocumentsViewModel(container.documentRepository) }
    val toolsViewModel: ToolsViewModel = viewModel { ToolsViewModel() }

    val shareDocument: (Document) -> Unit = { doc ->
        val file = File(doc.path)
        if (file.exists()) {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = doc.mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ${doc.name}"))
        }
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Documents.route,
        Screen.Tools.route,
        Screen.Settings.route
    )

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                ScanFlowBottomNav(
                    currentRoute = currentRoute,
                    onNavigate = { screen ->
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    onNavigateToScanner = { navController.navigate(Screen.Scanner.route) },
                    onNavigateToTool = { featureId -> navController.navigate(Screen.ToolAction.createRoute(featureId)) },
                    onNavigateToViewer = { docId -> navController.navigate(Screen.Viewer.createRoute(docId)) },
                    onNavigateToAiChat = { docId -> navController.navigate(Screen.AiChat.createRoute(docId)) },
                    onNavigateToCompare = { navController.navigate(Screen.Compare.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToDocuments = { navController.navigate(Screen.Documents.route) },
                    onShareDocument = shareDocument
                )
            }

            composable(Screen.Documents.route) {
                DocumentsScreen(
                    viewModel = documentsViewModel,
                    onNavigateToViewer = { docId -> navController.navigate(Screen.Viewer.createRoute(docId)) },
                    onShareDocument = shareDocument,
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Tools.route) {
                ToolsScreen(
                    viewModel = toolsViewModel,
                    onNavigateToTool = { featureId -> navController.navigate(Screen.ToolAction.createRoute(featureId)) },
                    onNavigateToScanner = { navController.navigate(Screen.Scanner.route) },
                    onNavigateToCompare = { navController.navigate(Screen.Compare.route) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen()
            }

            composable(Screen.Compare.route) {
                CompareScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(Screen.Scanner.route) {
                val scannerViewModel: ScannerViewModel = viewModel { ScannerViewModel(container) }
                ScannerScreen(
                    viewModel = scannerViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onDocumentCreated = { filePath ->
                        navController.popBackStack()
                        // navigate back to home/documents
                    }
                )
            }

            composable(
                route = Screen.Viewer.route,
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("docId") ?: ""
                val viewerViewModel: ViewerViewModel = viewModel(key = docId) { ViewerViewModel(container) }
                DocumentViewerScreen(
                    documentId = docId,
                    viewModel = viewerViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToAiChat = { id -> navController.navigate(Screen.AiChat.createRoute(id)) },
                    onShare = shareDocument
                )
            }

            composable(
                route = Screen.ToolAction.route,
                arguments = listOf(navArgument("featureId") { type = NavType.StringType })
            ) { backStackEntry ->
                val featureId = backStackEntry.arguments?.getString("featureId") ?: ""
                val toolViewModel: ToolActionViewModel = viewModel(key = featureId) { ToolActionViewModel(container) }
                ToolActionScreen(
                    featureId = featureId,
                    viewModel = toolViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToViewer = { docId -> navController.navigate(Screen.Viewer.createRoute(docId)) }
                )
            }

            composable(
                route = Screen.AiChat.route,
                arguments = listOf(navArgument("docId") { type = NavType.StringType })
            ) { backStackEntry ->
                val docId = backStackEntry.arguments?.getString("docId") ?: ""
                val aiViewModel: AiViewModel = viewModel(key = docId) { AiViewModel(container) }
                AiChatScreen(
                    documentId = docId,
                    viewModel = aiViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}
