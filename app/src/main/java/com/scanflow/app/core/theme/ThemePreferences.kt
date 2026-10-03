package com.scanflow.app.core.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Available theme modes for ScanFlow application.
 */
enum class AppThemeMode(
    val title: String,
    val subtitle: String
) {
    SYSTEM(
        title = "Mengikuti Sistem",
        subtitle = "Otomatis menyesuaikan mode terang / gelap sesuai pengaturan perangkat"
    ),
    LIGHT(
        title = "Mode Terang",
        subtitle = "Tampilan bersih, jernih, dan cerah untuk pencahayaan kuat"
    ),
    DARK(
        title = "Mode Gelap",
        subtitle = "Warna obsidian pekat yang modern, nyaman di mata, dan hemat baterai"
    )
}

/**
 * Manages user theme preferences with persistent SharedPreferences and reactive StateFlow.
 */
class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadInitialThemeMode())
    val themeMode: StateFlow<AppThemeMode> = _themeMode.asStateFlow()

    private fun loadInitialThemeMode(): AppThemeMode {
        val savedName = prefs.getString(KEY_THEME_MODE, AppThemeMode.SYSTEM.name)
        return try {
            AppThemeMode.valueOf(savedName ?: AppThemeMode.SYSTEM.name)
        } catch (_: Exception) {
            AppThemeMode.SYSTEM
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    companion object {
        private const val PREFS_NAME = "scanflow_theme_prefs"
        private const val KEY_THEME_MODE = "app_theme_mode"
    }
}
