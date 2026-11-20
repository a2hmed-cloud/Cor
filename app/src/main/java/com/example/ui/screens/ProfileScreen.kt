package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.i18n.LocalCreovaStrings
import com.example.domain.model.PublicProfile
import com.example.ui.components.GoogleSignInActionButton
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalCreovaGlass
import com.google.firebase.auth.FirebaseUser

@Composable
fun ProfileScreen(
    currentUser: FirebaseUser?,
    myProfile: PublicProfile?,
    isSigningIn: Boolean,
    isAdmin: Boolean,
    savedCount: Int,
    unreadNotificationsCount: Int,
    onSignInClick: () -> Unit,
    onOpenSavedItems: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onOpenAdminModeration: () -> Unit,
    onUpdateProfile: (displayName: String, username: String, bio: String, specialty: String, onDone: () -> Unit) -> Unit,
    contentPadding: PaddingValues
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    var showEditDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = strings.navProfile,
                style = MaterialTheme.typography.headlineMedium,
                color = glass.textPrimary
            )
        }

        if (currentUser == null) {
            item {
                LiquidGlassCard(elevated = true) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = strings.profileGuestTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = glass.textPrimary
                        )
                        Text(
                            text = strings.profileGuestSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                        GoogleSignInActionButton(
                            isLoading = isSigningIn,
                            onSignInClick = onSignInClick,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            item {
                val displayName = myProfile?.displayName?.ifBlank { currentUser.displayName ?: "Creator" }
                    ?: currentUser.displayName ?: "Creator"
                val username = myProfile?.username.orEmpty().ifBlank {
                    currentUser.email?.substringBefore("@").orEmpty().ifBlank { "creator" }
                }
                val avatarUrl = myProfile?.avatarUrl?.ifBlank { currentUser.photoUrl?.toString().orEmpty() }
                    ?: currentUser.photoUrl?.toString().orEmpty()

                LiquidGlassCard(elevated = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(glass.violetAccent, glass.cyanAccent))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = avatarUrl,
                                        contentDescription = displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = displayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "@$username",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = glass.cyanAccent
                                )
                                val specialtyText = myProfile?.specialty.orEmpty()
                                if (specialtyText.isNotBlank()) {
                                    Text(
                                        text = specialtyText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.violetAccent
                                    )
                                }
                            }
                        }

                        val bioText = myProfile?.bio.orEmpty()
                        if (bioText.isNotBlank()) {
                            Text(
                                text = bioText,
                                style = MaterialTheme.typography.bodyMedium,
                                color = glass.textSecondary
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ProfileStatPill(value = "${myProfile?.followersCount ?: 0}", label = strings.followers)
                            ProfileStatPill(value = "${myProfile?.postsCount ?: 0}", label = strings.postsLabel)
                            ProfileStatPill(value = "${myProfile?.resourcesCount ?: 0}", label = strings.resourcesLabel)
                        }

                        OutlinedButton(
                            onClick = { showEditDialog = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("edit_profile_button")
                        ) {
                            Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            Text(strings.editProfile)
                        }
                    }
                }
            }
        }

        item {
            ProfileNavRow(
                icon = Icons.Filled.Bookmark,
                title = strings.savedItemsTitle,
                badge = if (savedCount > 0) "$savedCount" else null,
                testTag = "profile_saved_items_row",
                onClick = onOpenSavedItems
            )
        }
        item {
            ProfileNavRow(
                icon = Icons.Filled.Notifications,
                title = strings.notificationsTitle,
                badge = if (unreadNotificationsCount > 0) "$unreadNotificationsCount" else null,
                testTag = "profile_notifications_row",
                onClick = onOpenNotifications
            )
        }
        item {
            ProfileNavRow(
                icon = Icons.Filled.AutoAwesome,
                title = strings.aiTitle,
                badge = null,
                testTag = "profile_ai_row",
                onClick = onOpenAiAssistant
            )
        }
        item {
            ProfileNavRow(
                icon = Icons.Filled.Settings,
                title = strings.settingsTitle,
                badge = null,
                testTag = "profile_settings_row",
                onClick = onOpenSettings
            )
        }

        if (isAdmin) {
            item {
                ProfileNavRow(
                    icon = Icons.Filled.AdminPanelSettings,
                    title = strings.moderationTitle,
                    badge = "Admin",
                    testTag = "profile_admin_row",
                    onClick = onOpenAdminModeration
                )
            }
        }
    }

    if (showEditDialog && currentUser != null) {
        var nameInput by remember(myProfile) {
            mutableStateOf(myProfile?.displayName?.ifBlank { currentUser.displayName.orEmpty() } ?: currentUser.displayName.orEmpty())
        }
        var usernameInput by remember(myProfile) {
            mutableStateOf(myProfile?.username.orEmpty())
        }
        var bioInput by remember(myProfile) {
            mutableStateOf(myProfile?.bio.orEmpty())
        }
        var specialtyInput by remember(myProfile) {
            mutableStateOf(myProfile?.specialty.orEmpty())
        }

        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            containerColor = glass.glassElevated,
            titleContentColor = glass.textPrimary,
            textContentColor = glass.textSecondary,
            title = { Text(strings.editProfile) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text(strings.displayNameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { usernameInput = it },
                        label = { Text(strings.usernameLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = specialtyInput,
                        onValueChange = { specialtyInput = it },
                        label = { Text(strings.specialtyLabel) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = bioInput,
                        onValueChange = { bioInput = it },
                        label = { Text(strings.bioLabel) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (nameInput.trim().length >= 2) {
                            onUpdateProfile(
                                nameInput.trim(),
                                usernameInput.trim(),
                                bioInput.trim(),
                                specialtyInput.trim()
                            ) {
                                showEditDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = glass.violetAccent)
                ) {
                    Text(strings.saveChanges)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(strings.cancel, color = glass.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun ProfileStatPill(value: String, label: String) {
    val glass = LocalCreovaGlass.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = glass.textPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = glass.textSecondary
        )
    }
}

@Composable
private fun ProfileNavRow(
    icon: ImageVector,
    title: String,
    badge: String?,
    testTag: String,
    onClick: () -> Unit
) {
    val glass = LocalCreovaGlass.current
    LiquidGlassCard(
        onClick = onClick,
        modifier = Modifier.testTag(testTag),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = glass.cyanAccent
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = glass.textPrimary,
                modifier = Modifier.weight(1f)
            )
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(glass.violetAccent.copy(alpha = 0.22f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.cyanAccent
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = glass.textSecondary
            )
        }
    }
}
