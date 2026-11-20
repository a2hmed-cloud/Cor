package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.core.security.SafeUrlLauncher
import com.example.data.preferences.GeminiModelOption
import com.example.domain.model.CreativeResource
import com.example.domain.model.CreativeTool
import com.example.ui.components.CreovaTopBar
import com.example.ui.components.LiquidGlassAiOrb
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid
import com.example.ui.viewmodel.AiAssistantUiState
import com.example.ui.viewmodel.AiChatMessage

@Composable
fun AiAssistantScreen(
    language: AppLanguage,
    modelOption: GeminiModelOption,
    messages: List<AiChatMessage>,
    uiState: AiAssistantUiState,
    tools: List<CreativeTool>,
    resources: List<CreativeResource>,
    initialPrompt: String?,
    onBack: () -> Unit,
    onSendPrompt: (prompt: String, structured: Boolean) -> Unit,
    onClearChat: () -> Unit,
    onSelectModel: (GeminiModelOption) -> Unit,
    onShowError: (String) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val context = LocalContext.current
    val isArabic = language == AppLanguage.AR

    var inputText by remember { mutableStateOf("") }
    var structuredMode by remember { mutableStateOf(false) }

    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank() && messages.isEmpty()) {
            onSendPrompt(initialPrompt, false)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        CreovaTopBar(
            title = strings.aiTitle,
            onBack = onBack,
            actions = {
                if (messages.isNotEmpty()) {
                    IconButton(onClick = onClearChat) {
                        Icon(
                            imageVector = Icons.Filled.DeleteSweep,
                            contentDescription = strings.clearAll,
                            tint = glass.textSecondary
                        )
                    }
                }
            }
        )

        // Model selector & structured toggle
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(GeminiModelOption.entries) { option ->
                    LiquidGlassPill(
                        text = if (isArabic) option.labelAr else option.labelEn,
                        selected = modelOption == option,
                        onClick = { onSelectModel(option) }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.aiStructuredHeader,
                    style = MaterialTheme.typography.labelLarge,
                    color = glass.textSecondary
                )
                Switch(
                    checked = structuredMode,
                    onCheckedChange = { structuredMode = it }
                )
            }
            Text(
                text = strings.aiDisclaimer,
                style = MaterialTheme.typography.labelSmall,
                color = glass.cyanAccent
            )
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && uiState is AiAssistantUiState.Idle) {
                item {
                    LiquidGlassCard(elevated = true) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                LiquidGlassAiOrb(modifier = Modifier.fillMaxSize())
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = androidx.compose.ui.graphics.Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                            Text(strings.aiSubtitle, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                val quickPrompts = listOf(
                                    strings.aiQuickToolFinder,
                                    strings.aiQuickFreeAlt,
                                    strings.aiQuickReelsWorkflow,
                                    strings.aiQuickArabicFonts
                                )
                                items(quickPrompts) { qp ->
                                    LiquidGlassPill(
                                        text = qp,
                                        selected = false,
                                        onClick = { onSendPrompt(qp, structuredMode) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            items(messages, key = { it.id }) { msg ->
                LiquidGlassCard(
                    elevated = !msg.isFromUser,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (msg.isFromUser) strings.navProfile else strings.aiTitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (msg.isFromUser) glass.violetAccent else glass.cyanAccent
                        )
                        Text(
                            text = msg.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textPrimary
                        )
                        msg.structuredResponse?.let { structured ->
                            if (structured.workflowSteps.isNotEmpty()) {
                                structured.workflowSteps.forEachIndexed { idx, step ->
                                    Text(
                                        text = "${idx + 1}. $step",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary
                                    )
                                }
                            }
                            structured.picks.forEach { pick ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = glass.violetAccent.copy(alpha = 0.14f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(pick.name, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                                            Text(pick.pricing, style = MaterialTheme.typography.labelSmall, color = glass.cyanAccent)
                                        }
                                        Text(pick.reason, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary)
                                        if (pick.verifiedUrl.isNotBlank()) {
                                            OutlinedButton(
                                                onClick = {
                                                    val ok = SafeUrlLauncher.openSafeCustomTab(context, pick.verifiedUrl, glass.canvas.toArgb())
                                                    if (!ok) onShowError(strings.urlBlockedError)
                                                },
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Filled.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                                Text(strings.openOfficialSite, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            when (uiState) {
                is AiAssistantUiState.Loading -> {
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = glass.cyanAccent)
                            Text(strings.loadingLabel, color = glass.textSecondary)
                        }
                    }
                }
                is AiAssistantUiState.Streaming -> {
                    item {
                        LiquidGlassCard(elevated = true) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(strings.aiTitle, style = MaterialTheme.typography.labelSmall, color = glass.cyanAccent)
                                Text(uiState.partialText, style = MaterialTheme.typography.bodyMedium, color = glass.textPrimary)
                            }
                        }
                    }
                }
                is AiAssistantUiState.Error -> {
                    item {
                        LiquidGlassCard {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(uiState.message, color = RosePaid, style = MaterialTheme.typography.bodyMedium)
                                Button(onClick = { onSendPrompt(uiState.lastPrompt, structuredMode) }) {
                                    Text(strings.aiRetry)
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }

        // Prompt Input Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text(strings.aiInputHint) },
                maxLines = 3,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("ai_prompt_input")
            )
            IconButton(
                onClick = {
                    val prompt = inputText.trim()
                    if (prompt.isNotEmpty() && uiState !is AiAssistantUiState.Loading && uiState !is AiAssistantUiState.Streaming) {
                        inputText = ""
                        onSendPrompt(prompt, structuredMode)
                    }
                },
                modifier = Modifier.testTag("ai_send_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = strings.submit,
                    tint = glass.cyanAccent
                )
            }
        }
    }
}
