package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.core.security.SafeUrlLauncher
import com.example.domain.model.ResourceCategory
import com.example.ui.components.CreovaTopBar
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid

@Composable
fun SubmitResourceScreen(
    language: AppLanguage,
    onBack: () -> Unit,
    onSubmit: (
        title: String,
        titleAr: String,
        description: String,
        descriptionAr: String,
        category: String,
        tags: List<String>,
        fileFormat: String,
        fileSizeLabel: String,
        licenseInfo: String,
        downloadUrl: String
    ) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val isArabic = language == AppLanguage.AR

    var selectedCategory by remember { mutableStateOf(ResourceCategory.FONTS) }
    var title by remember { mutableStateOf("") }
    var titleAr by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tagsRaw by remember { mutableStateOf("") }
    var format by remember { mutableStateOf("TTF") }
    var fileSize by remember { mutableStateOf("4 MB") }
    var license by remember { mutableStateOf("Creative Commons / Free Commercial") }
    var downloadUrl by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        CreovaTopBar(title = strings.submitResource, onBack = onBack)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(ResourceCategory.entries) { cat ->
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
                        value = titleAr,
                        onValueChange = { titleAr = it },
                        label = { Text("الاسم بالعربية") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("English Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(strings.postDescriptionLabel) },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = format,
                        onValueChange = { format = it },
                        label = { Text(strings.fileFormatLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = fileSize,
                        onValueChange = { fileSize = it },
                        label = { Text(strings.fileSizeLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = license,
                        onValueChange = { license = it },
                        label = { Text(strings.licenseLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tagsRaw,
                        onValueChange = { tagsRaw = it },
                        label = { Text(strings.postTagsLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = downloadUrl,
                        onValueChange = { downloadUrl = it },
                        label = { Text("HTTPS Source / Download URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (validationError != null) {
                        Text(
                            text = validationError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = RosePaid
                        )
                    }

                    Button(
                        onClick = {
                            val effectiveTitle = title.trim().ifBlank { titleAr.trim() }
                            val effectiveAr = titleAr.trim().ifBlank { title.trim() }
                            if (effectiveTitle.length < 2 || description.trim().length < 4) {
                                validationError = "Please enter a valid title and description."
                                return@Button
                            }
                            if (!SafeUrlLauncher.isSafeHttpsUrl(downloadUrl)) {
                                validationError = strings.urlBlockedError
                                return@Button
                            }
                            validationError = null
                            val tags = tagsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            onSubmit(
                                effectiveTitle,
                                effectiveAr,
                                description.trim(),
                                description.trim(),
                                selectedCategory.id,
                                tags,
                                format.trim(),
                                fileSize.trim(),
                                license.trim(),
                                downloadUrl.trim()
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
                            .testTag("confirm_submit_resource_button")
                    ) {
                        Text(strings.submit, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
