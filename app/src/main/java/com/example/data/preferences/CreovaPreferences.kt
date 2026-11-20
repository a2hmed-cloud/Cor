package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.creovaDataStore: DataStore<Preferences> by preferencesDataStore(name = "creova_prefs")

enum class GeminiModelOption(
    val modelId: String,
    val labelAr: String,
    val labelEn: String,
    val badgeAr: String,
    val badgeEn: String
) {
    FLASH_DEFAULT(
        modelId = "gemini-3.8-flash",
        labelAr = "Gemini 3.8 Flash",
        labelEn = "Gemini 3.8 Flash",
        badgeAr = "الافتراضي المتوازن",
        badgeEn = "Default Balanced"
    ),
    FLASH_LITE(
        modelId = "gemini-3.5-flash-lite",
        labelAr = "Gemini 3.5 Flash-Lite",
        labelEn = "Gemini 3.5 Flash-Lite",
        badgeAr = "أسرع استجابة",
        badgeEn = "Ultra Fast"
    ),
    ARGON_REASONING(
        modelId = "gemini-4-argon",
        labelAr = "Gemini 4 Argon",
        labelEn = "Gemini 4 Argon",
        badgeAr = "تحليل عميق",
        badgeEn = "Deep Reasoning"
    );

    companion object {
        fun fromId(id: String?): GeminiModelOption =
            entries.firstOrNull { it.modelId == id } ?: FLASH_DEFAULT
    }
}

data class UserPrefsSnapshot(
    val language: AppLanguage = AppLanguage.AR,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val aiModel: GeminiModelOption = GeminiModelOption.FLASH_DEFAULT,
    val reducedMotion: Boolean = false,
    val recentSearches: List<String> = emptyList()
)

class CreovaPreferences(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val AI_MODEL = stringPreferencesKey("ai_model")
        val REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val RECENT_SEARCHES = stringPreferencesKey("recent_searches")
    }

    val prefsFlow: Flow<UserPrefsSnapshot> = context.creovaDataStore.data.map { prefs ->
        val reducedMotionPref = prefs[Keys.REDUCED_MOTION]
        val rawSearches = prefs[Keys.RECENT_SEARCHES].orEmpty()
        UserPrefsSnapshot(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = AppThemeMode.fromValue(prefs[Keys.THEME_MODE]),
            aiModel = GeminiModelOption.fromId(prefs[Keys.AI_MODEL]),
            reducedMotion = reducedMotionPref ?: false,
            recentSearches = rawSearches.split("||").filter { it.isNotBlank() }
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.creovaDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.creovaDataStore.edit { it[Keys.THEME_MODE] = themeMode.value }
    }

    suspend fun setAiModel(model: GeminiModelOption) {
        context.creovaDataStore.edit { it[Keys.AI_MODEL] = model.modelId }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        context.creovaDataStore.edit { it[Keys.REDUCED_MOTION] = enabled }
    }

    suspend fun addRecentSearch(query: String) {
        val clean = query.trim().take(60)
        if (clean.isBlank()) return
        context.creovaDataStore.edit { prefs ->
            val current = prefs[Keys.RECENT_SEARCHES].orEmpty()
                .split("||")
                .filter { it.isNotBlank() && !it.equals(clean, ignoreCase = true) }
            val updated = (listOf(clean) + current).take(8)
            prefs[Keys.RECENT_SEARCHES] = updated.joinToString("||")
        }
    }

    suspend fun clearRecentSearches() {
        context.creovaDataStore.edit { it.remove(Keys.RECENT_SEARCHES) }
    }
}
