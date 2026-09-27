package com.scanflow.app.ui.navigation

sealed class Screen(val route: String, val title: String) {
    object Home : Screen("home", "Home")
    object Documents : Screen("documents", "Documents")
    object Tools : Screen("tools", "Tools")
    object Settings : Screen("settings", "Settings")
    object Scanner : Screen("scanner", "Scanner")
    object Viewer : Screen("viewer/{docId}", "Document Viewer") {
        fun createRoute(docId: String) = "viewer/$docId"
    }
    object ToolAction : Screen("tool_action/{featureId}", "Tool Action") {
        fun createRoute(featureId: String) = "tool_action/$featureId"
    }
    object AiChat : Screen("ai_chat/{docId}", "Ask PDF") {
        fun createRoute(docId: String) = "ai_chat/$docId"
    }
}
