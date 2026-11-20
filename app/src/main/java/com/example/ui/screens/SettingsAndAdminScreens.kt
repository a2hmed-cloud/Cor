package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.AppThemeMode
import com.example.core.i18n.LocalCreovaStrings
import com.example.data.preferences.GeminiModelOption
import com.example.data.preferences.UserPrefsSnapshot
import com.example.domain.model.CreativeResource
import com.example.domain.model.ModerationReport
import com.example.ui.components.CreovaTopBar
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.EmeraldFree
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    prefs: UserPrefsSnapshot,
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSetLanguage: (AppLanguage) -> Unit,
    onSetThemeMode: (AppThemeMode) -> Unit,
    onSetAiModel: (GeminiModelOption) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val isArabic = prefs.language == AppLanguage.AR
    var confirmDeleteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CreovaTopBar(title = strings.settingsTitle, onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Language Switcher
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.languageSetting, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassPill(
                                text = strings.arabicLabel,
                                selected = prefs.language == AppLanguage.AR,
                                onClick = { onSetLanguage(AppLanguage.AR) },
                                modifier = Modifier.testTag("settings_lang_ar")
                            )
                            LiquidGlassPill(
                                text = strings.englishLabel,
                                selected = prefs.language == AppLanguage.EN,
                                onClick = { onSetLanguage(AppLanguage.EN) },
                                modifier = Modifier.testTag("settings_lang_en")
                            )
                        }
                    }
                }
            }

            // Theme Mode
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.themeSetting, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LiquidGlassPill(
                                text = strings.themeSystem,
                                selected = prefs.themeMode == AppThemeMode.SYSTEM,
                                onClick = { onSetThemeMode(AppThemeMode.SYSTEM) },
                                modifier = Modifier.testTag("settings_theme_system")
                            )
                            LiquidGlassPill(
                                text = strings.themeDark,
                                selected = prefs.themeMode == AppThemeMode.DARK,
                                onClick = { onSetThemeMode(AppThemeMode.DARK) },
                                modifier = Modifier.testTag("settings_theme_dark")
                            )
                            LiquidGlassPill(
                                text = strings.themeLight,
                                selected = prefs.themeMode == AppThemeMode.LIGHT,
                                onClick = { onSetThemeMode(AppThemeMode.LIGHT) },
                                modifier = Modifier.testTag("settings_theme_light")
                            )
                        }
                    }
                }
            }

            // Gemini AI Model Setting
            item {
                LiquidGlassCard {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.aiModelSetting, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            GeminiModelOption.entries.forEach { opt ->
                                val label = if (isArabic) "${opt.labelAr} • ${opt.badgeAr}" else "${opt.labelEn} • ${opt.badgeEn}"
                                LiquidGlassPill(
                                    text = label,
                                    selected = prefs.aiModel == opt,
                                    onClick = { onSetAiModel(opt) }
                                )
                            }
                        }
                    }
                }
            }

            // Reduced Motion
            item {
                LiquidGlassCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.reducedMotionSetting,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = prefs.reducedMotion,
                            onCheckedChange = onSetReducedMotion,
                            modifier = Modifier.testTag("settings_reduced_motion_switch")
                        )
                    }
                }
            }

            if (isSignedIn) {
                item {
                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settings_sign_out_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                        Text(
                            text = strings.signOut,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                item {
                    Button(
                        onClick = { confirmDeleteDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RosePaid.copy(alpha = 0.18f),
                            contentColor = RosePaid
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("settings_delete_account_button")
                    ) {
                        Icon(Icons.Filled.DeleteForever, contentDescription = null)
                        Text(
                            text = strings.deleteAccount,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }

    if (confirmDeleteDialog) {
        AlertDialog(
            onDismissRequest = { confirmDeleteDialog = false },
            containerColor = glass.glassElevated,
            titleContentColor = glass.textPrimary,
            textContentColor = glass.textSecondary,
            title = { Text(strings.deleteAccountConfirmTitle) },
            text = { Text(strings.deleteAccountConfirmBody) },
            confirmButton = {
                Button(
                    onClick = {
                        confirmDeleteDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RosePaid)
                ) {
                    Text(strings.confirmDelete)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteDialog = false }) {
                    Text(strings.cancel, color = glass.textSecondary)
                }
            }
        )
    }
}

@Composable
fun AdminModerationScreen(
    isAdmin: Boolean,
    pendingResources: List<CreativeResource>,
    openReports: List<ModerationReport>,
    onBack: () -> Unit,
    onModerateResource: (resourceId: String, status: String) -> Unit,
    onUpdateReportStatus: (reportId: String, status: String) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CreovaTopBar(title = strings.moderationTitle, onBack = onBack)

        if (!isAdmin) {
            Column(modifier = Modifier.padding(16.dp)) {
                EmptyStateGlassCard(
                    title = strings.emptyTitle,
                    body = strings.emptyBody,
                    icon = Icons.Filled.AdminPanelSettings
                )
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (pendingResources.isEmpty() && openReports.isEmpty()) {
                item {
                    EmptyStateGlassCard(
                        title = strings.emptyTitle,
                        body = strings.emptyBody,
                        icon = Icons.Filled.AdminPanelSettings
                    )
                }
            }

            if (pendingResources.isNotEmpty()) {
                item {
                    Text(strings.libraryTitle, style = MaterialTheme.typography.titleMedium, color = glass.cyanAccent)
                }
                items(pendingResources, key = { "pr_${it.id}" }) { res ->
                    LiquidGlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(res.title, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                            Text(res.downloadUrl, style = MaterialTheme.typography.labelSmall, color = glass.cyanAccent)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onModerateResource(res.id, "approved") },
                                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldFree)
                                ) {
                                    Text(strings.approve)
                                }
                                OutlinedButton(onClick = { onModerateResource(res.id, "rejected") }) {
                                    Text(strings.reject, color = RosePaid)
                                }
                            }
                        }
                    }
                }
            }

            if (openReports.isNotEmpty()) {
                item {
                    Text(strings.report, style = MaterialTheme.typography.titleMedium, color = glass.cyanAccent)
                }
                items(openReports, key = { "rep_${it.id}" }) { rep ->
                    LiquidGlassCard {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("${rep.targetType} • ${rep.targetId}", style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                            Text(rep.reason, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { onUpdateReportStatus(rep.id, "resolved") },
                                    colors = ButtonDefaults.buttonColors(containerColor = glass.violetAccent)
                                ) {
                                    Text(strings.resolveReport)
                                }
                                OutlinedButton(onClick = { onUpdateReportStatus(rep.id, "dismissed") }) {
                                    Text(strings.dismissReport, color = glass.textSecondary)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
