package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.core.media.ImageCompressor
import com.example.core.security.SafeUrlLauncher
import com.example.domain.model.PostType
import com.example.domain.model.ToolCategory
import com.example.ui.components.CreovaTopBar
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid
import kotlinx.coroutines.launch

@Composable
fun CreatePostScreen(
    language: AppLanguage,
    onBack: () -> Unit,
    onPublish: (
        postType: String,
        category: String,
        title: String,
        description: String,
        tags: List<String>,
        mediaDataUrl: String?,
        linkedUrl: String?
    ) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isArabic = language == AppLanguage.AR

    var selectedType by remember { mutableStateOf(PostType.DESIGN_SHOWCASE) }
    var selectedCategory by remember { mutableStateOf(ToolCategory.GRAPHIC_DESIGN) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagsRaw by remember { mutableStateOf("") }
    var linkedUrl by remember { mutableStateOf("") }
    var mediaDataUrl by remember { mutableStateOf<String?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                ImageCompressor.compressUriToDataUrl(context, uri)
                    .onSuccess {
                        mediaDataUrl = it
                        errorMsg = null
                    }
                    .onFailure {
                        errorMsg = it.localizedMessage ?: "Unable to attach image."
                    }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        CreovaTopBar(title = strings.createPost, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(strings.postTypeLabel, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PostType.entries) { type ->
                    LiquidGlassPill(
                        text = if (isArabic) type.labelAr else type.labelEn,
                        selected = selectedType == type,
                        onClick = { selectedType = type }
                    )
                }
            }

            Text(strings.postCategoryLabel, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ToolCategory.entries) { cat ->
                    LiquidGlassPill(
                        text = if (isArabic) cat.labelAr else cat.labelEn,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = cat }
                    )
                }
            }

            LiquidGlassCard {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(strings.postTitleLabel) },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_post_title_input")
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(strings.postDescriptionLabel) },
                        minLines = 4,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_post_description_input")
                    )
                    OutlinedTextField(
                        value = tagsRaw,
                        onValueChange = { tagsRaw = it },
                        label = { Text(strings.postTagsLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = linkedUrl,
                        onValueChange = { linkedUrl = it },
                        label = { Text(strings.linkedUrlOptional) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = null)
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text(strings.attachImage)
                        }
                        if (mediaDataUrl != null) {
                            TextButton(onClick = { mediaDataUrl = null }) {
                                Text(strings.removeImage, color = RosePaid)
                            }
                        }
                    }

                    if (mediaDataUrl != null) {
                        AsyncImage(
                            model = mediaDataUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }

                    if (errorMsg != null) {
                        Text(errorMsg!!, color = RosePaid, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            if (title.trim().length < 2 || description.trim().length < 2) {
                                errorMsg = "Title and description must be at least 2 characters."
                                return@Button
                            }
                            val urlClean = linkedUrl.trim().takeIf { it.isNotBlank() }
                            if (urlClean != null && !SafeUrlLauncher.isSafeHttpsUrl(urlClean)) {
                                errorMsg = strings.urlBlockedError
                                return@Button
                            }
                            errorMsg = null
                            val tags = tagsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            onPublish(
                                selectedType.wireValue,
                                selectedCategory.id,
                                title.trim(),
                                description.trim(),
                                tags,
                                mediaDataUrl,
                                urlClean
                            )
                        },
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = glass.violetAccent,
                            contentColor = androidx.compose.ui.graphics.Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("publish_post_button")
                    ) {
                        Text(strings.publishPost, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
