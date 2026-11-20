package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.core.security.SafeUrlLauncher
import com.example.domain.model.CreativeTool
import com.example.domain.model.ToolCategory
import com.example.ui.components.CreovaTopBar
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.PricingBadge
import com.example.ui.components.VerifiedPill
import com.example.ui.theme.LocalCreovaGlass

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolDetailScreen(
    language: AppLanguage,
    tool: CreativeTool?,
    isSaved: Boolean,
    onBack: () -> Unit,
    onToggleSave: (CreativeTool) -> Unit,
    onToolOpened: (CreativeTool) -> Unit,
    onAskAiAboutTool: (String) -> Unit,
    onShowError: (String) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val context = LocalContext.current
    val isArabic = language == AppLanguage.AR

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        CreovaTopBar(
            title = if (tool != null) (if (isArabic) tool.nameAr else tool.name) else strings.toolsTitle,
            onBack = onBack,
            actions = {
                if (tool != null) {
                    IconButton(onClick = { onToggleSave(tool) }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = strings.save,
                            tint = if (isSaved) glass.cyanAccent else glass.textPrimary
                        )
                    }
                }
            }
        )

        if (tool == null) return@Column

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LiquidGlassCard(elevated = true) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PricingBadge(tool.pricingTier)
                        if (tool.verified) VerifiedPill()
                    }
                    Text(
                        text = if (isArabic) "${tool.nameAr} (${tool.name})" else tool.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = glass.textPrimary
                    )
                    Text(
                        text = if (isArabic) tool.descriptionAr else tool.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = glass.textSecondary
                    )
                    Text(
                        text = "${strings.lastVerifiedLabel}: ${tool.lastVerifiedDate}",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.cyanAccent
                    )
                }
            }

            LiquidGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.platformsLabel,
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        tool.platforms.forEach { platform ->
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = glass.violetAccent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = platform,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = glass.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                        tool.categories.mapNotNull { ToolCategory.fromId(it) }.forEach { cat ->
                            androidx.compose.material3.Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = glass.cyanAccent.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = if (isArabic) cat.labelAr else cat.labelEn,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = glass.cyanAccent,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            LiquidGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = glass.cyanAccent
                        )
                        Text(
                            text = "${tool.ratingAvg} / 5.0  •  ${tool.savesCount} ${strings.saved}",
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                    }
                    Text(
                        text = strings.creovaEngagementNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
            }

            Button(
                onClick = {
                    val launched = SafeUrlLauncher.openSafeCustomTab(
                        context = context,
                        rawUrl = tool.officialUrl,
                        toolbarColorArgb = glass.canvas.toArgb()
                    )
                    if (launched) {
                        onToolOpened(tool)
                    } else {
                        onShowError(strings.urlBlockedError)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = glass.violetAccent,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("open_tool_website_button")
            ) {
                Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(strings.openOfficialSite, style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = {
                    val prompt = if (isArabic) {
                        "احكيلي عن مميزات ${tool.name} وإيه البدائل المجانية أو المناسبة ليه في CREOVA؟"
                    } else {
                        "Tell me about ${tool.name} strengths and verified free alternatives on CREOVA."
                    }
                    onAskAiAboutTool(prompt)
                },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = glass.cyanAccent)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(strings.aiAskCta, color = glass.textPrimary)
            }
        }
    }
}
