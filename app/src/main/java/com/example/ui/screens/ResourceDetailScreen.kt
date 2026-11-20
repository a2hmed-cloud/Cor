package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.core.security.SafeUrlLauncher
import com.example.domain.model.CreativeResource
import com.example.domain.model.ResourceCategory
import com.example.ui.components.CreovaTopBar
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResourceDetailScreen(
    language: AppLanguage,
    resource: CreativeResource?,
    isSaved: Boolean,
    isLiked: Boolean,
    onBack: () -> Unit,
    onToggleSave: (CreativeResource) -> Unit,
    onToggleLike: (CreativeResource) -> Unit,
    onReportResource: (CreativeResource) -> Unit,
    onResourceViewed: (CreativeResource) -> Unit,
    onShowError: (String) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val context = LocalContext.current
    val isArabic = language == AppLanguage.AR

    LaunchedEffect(resource?.id) {
        resource?.let { onResourceViewed(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        CreovaTopBar(
            title = if (resource != null) (if (isArabic) resource.titleAr else resource.title) else strings.libraryTitle,
            onBack = onBack,
            actions = {
                if (resource != null) {
                    IconButton(onClick = { onToggleLike(resource) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) RosePaid else glass.textPrimary
                        )
                    }
                    IconButton(onClick = { onToggleSave(resource) }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = strings.save,
                            tint = if (isSaved) glass.cyanAccent else glass.textPrimary
                        )
                    }
                }
            }
        )

        if (resource == null) return@Column

        val catObj = ResourceCategory.fromId(resource.category)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            LiquidGlassCard(elevated = true) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = if (isArabic) catObj.labelAr else catObj.labelEn,
                        style = MaterialTheme.typography.labelLarge,
                        color = glass.cyanAccent
                    )
                    Text(
                        text = if (isArabic) resource.titleAr else resource.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = glass.textPrimary
                    )
                    Text(
                        text = if (isArabic) resource.descriptionAr else resource.description,
                        style = MaterialTheme.typography.bodyLarge,
                        color = glass.textSecondary
                    )
                    Text(
                        text = "${resource.creatorName} • ${resource.likesCount} ♥ • ${resource.savesCount} ${strings.saved}",
                        style = MaterialTheme.typography.labelMedium,
                        color = glass.violetAccent
                    )
                }
            }

            LiquidGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.fileFormatLabel, color = glass.textSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(resource.fileFormat, color = glass.textPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.fileSizeLabel, color = glass.textSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(resource.fileSizeLabel, color = glass.textPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(strings.licenseLabel, color = glass.textSecondary, style = MaterialTheme.typography.bodySmall)
                        Text(resource.licenseInfo, color = glass.cyanAccent, style = MaterialTheme.typography.labelLarge)
                    }
                    if (resource.tags.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            resource.tags.forEach { tag ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = glass.violetAccent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.textPrimary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    val ok = SafeUrlLauncher.openSafeCustomTab(
                        context = context,
                        rawUrl = resource.downloadUrl,
                        toolbarColorArgb = glass.canvas.toArgb()
                    )
                    if (!ok) onShowError(strings.urlBlockedError)
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = glass.violetAccent,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("open_resource_source_button")
            ) {
                Icon(Icons.Filled.Download, contentDescription = null)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(strings.openResourceSource, style = MaterialTheme.typography.labelLarge)
            }

            OutlinedButton(
                onClick = { onReportResource(resource) },
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.Flag, contentDescription = null, tint = RosePaid)
                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                Text(strings.report, color = RosePaid)
            }
        }
    }
}
