package com.example.ui.screens

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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

private fun readResourceFileMetadata(context: Context, uri: Uri): Pair<String, Long> {
    var name = "resource"
    var size = 0L
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: name
            if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
        }
    }
    return name to size
}

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
        downloadUrl: String,
        fileUri: Uri?,
        mimeType: String?,
        onProgress: (Float) -> Unit,
        onFinished: (Boolean) -> Unit
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
    val context = LocalContext.current
    var selectedFileUri by remember { mutableStateOf<Uri?>(null) }
    var selectedFileName by remember { mutableStateOf<String?>(null) }
    var selectedMimeType by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var uploadProgress by remember { mutableStateOf(0f) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val metadata = readResourceFileMetadata(context, uri)
            val extension = metadata.first.substringAfterLast('.', "").lowercase()
            val mime = when (extension) {
                "pdf" -> "application/pdf"
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "webp" -> "image/webp"
                "ttf" -> "font/ttf"
                "otf" -> "font/otf"
                "woff" -> "font/woff"
                "woff2" -> "font/woff2"
                else -> null
            }
            if (mime == null) {
                validationError = if (isArabic) "نوع الملف غير مدعوم. اختار PDF أو صورة أو خط." else "Unsupported file. Choose PDF, an image, or a font."
            } else if (metadata.second > 25L * 1024L * 1024L) {
                validationError = if (isArabic) "الحد الأقصى لحجم الملف 25 ميجابايت." else "Maximum file size is 25 MB."
            } else {
                selectedFileUri = uri
                selectedFileName = metadata.first
                selectedMimeType = mime
                format = extension.uppercase()
                if (metadata.second > 0L) fileSize = String.format(java.util.Locale.US, "%.2f MB", metadata.second / (1024.0 * 1024.0))
                downloadUrl = ""
                validationError = null
            }
        }
    }

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
                    Text(if (isArabic) "مصدر الملف" else "File source", style = MaterialTheme.typography.titleSmall)
                    OutlinedButton(
                        onClick = {
                            selectedFileUri = null
                            selectedFileName = null
                            selectedMimeType = null
                        },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isArabic) "استخدام رابط خارجي" else "Use external download link") }
                    Button(
                        onClick = { filePicker.launch(arrayOf("*/*")) },
                        enabled = !isSubmitting,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (isArabic) "اختيار ملف من الجهاز" else "Choose file from device") }
                    if (selectedFileUri != null) {
                        Text(selectedFileName.orEmpty(), style = MaterialTheme.typography.bodyMedium)
                        Text(if (isArabic) "سيتم رفع الملف إلى Firebase Storage" else "This file will be uploaded to Firebase Storage", style = MaterialTheme.typography.bodySmall)
                    } else {
                        OutlinedTextField(
                            value = downloadUrl,
                            onValueChange = { downloadUrl = it },
                            label = { Text("HTTPS Source / Download URL") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (isSubmitting && selectedFileUri != null) {
                        LinearProgressIndicator(progress = { uploadProgress }, modifier = Modifier.fillMaxWidth())
                        Text("${(uploadProgress * 100).toInt()}%")
                    }


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
                            if (selectedFileUri == null && !SafeUrlLauncher.isSafeHttpsUrl(downloadUrl)) {
                                validationError = strings.urlBlockedError
                                return@Button
                            }
                            if (isSubmitting) return@Button
                            validationError = null
                            val tags = tagsRaw.split(",").map { it.trim() }.filter { it.isNotBlank() }
                            isSubmitting = true
                            uploadProgress = 0f
                            onSubmit(
                                effectiveTitle, effectiveAr, description.trim(), description.trim(),
                                selectedCategory.id, tags, format.trim(), fileSize.trim(), license.trim(),
                                downloadUrl.trim(), selectedFileUri, selectedMimeType,
                                { progress -> uploadProgress = progress.coerceIn(0f, 1f) },
                                { success -> isSubmitting = false; if (!success) uploadProgress = 0f }
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
                        Text(if (isSubmitting) (if (isArabic) "جاري الإرسال..." else "Submitting...") else strings.submit, style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}
