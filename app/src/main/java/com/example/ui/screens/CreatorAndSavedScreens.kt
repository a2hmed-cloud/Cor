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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.domain.model.CommunityPost
import com.example.domain.model.CreativeResource
import com.example.domain.model.NotificationItem
import com.example.domain.model.PublicProfile
import com.example.domain.model.SavedItem
import com.example.ui.components.CreovaTopBar
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid

@Composable
fun CreatorProfileScreen(
    language: AppLanguage,
    creator: PublicProfile?,
    posts: List<CommunityPost>,
    resources: List<CreativeResource>,
    currentUserId: String?,
    isFollowing: Boolean,
    isBlocked: Boolean,
    onBack: () -> Unit,
    onToggleFollow: (String) -> Unit,
    onToggleBlock: (String) -> Unit,
    onReportCreator: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenResource: (String) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val isArabic = language == AppLanguage.AR

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CreovaTopBar(
            title = creator?.displayName ?: strings.navProfile,
            onBack = onBack
        )

        if (creator == null) {
            Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                EmptyStateGlassCard(
                    title = strings.emptyTitle,
                    body = strings.emptyBody,
                    icon = Icons.Filled.Person
                )
            }
            return@Column
        }

        val creatorPosts = posts.filter { it.authorId == creator.userId }
        val creatorResources = resources.filter { it.creatorId == creator.userId }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                LiquidGlassCard(elevated = true) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(glass.violetAccent, glass.cyanAccent))),
                                contentAlignment = Alignment.Center
                            ) {
                                if (creator.avatarUrl.isNotBlank()) {
                                    AsyncImage(
                                        model = creator.avatarUrl,
                                        contentDescription = creator.displayName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Text(
                                        text = creator.displayName.take(1).uppercase(),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = androidx.compose.ui.graphics.Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(creator.displayName, style = MaterialTheme.typography.titleLarge, color = glass.textPrimary)
                                Text("@${creator.username}", style = MaterialTheme.typography.labelLarge, color = glass.cyanAccent)
                                if (creator.specialty.isNotBlank()) {
                                    Text(creator.specialty, style = MaterialTheme.typography.labelSmall, color = glass.violetAccent)
                                }
                            }
                        }

                        if (creator.bio.isNotBlank()) {
                            Text(creator.bio, style = MaterialTheme.typography.bodyMedium, color = glass.textSecondary)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${creator.followersCount}", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                                Text(strings.followers, style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${creatorPosts.size}", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                                Text(strings.postsLabel, style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("${creatorResources.size}", style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                                Text(strings.resourcesLabel, style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                            }
                        }

                        if (currentUserId != creator.userId) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onToggleFollow(creator.userId) },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isFollowing) glass.glassElevated else glass.violetAccent,
                                        contentColor = if (isFollowing) glass.cyanAccent else androidx.compose.ui.graphics.Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .testTag("creator_follow_button")
                                ) {
                                    Text(if (isFollowing) strings.following else strings.follow)
                                }
                                OutlinedButton(
                                    onClick = { onToggleBlock(creator.userId) },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Filled.Block, contentDescription = null, modifier = Modifier.size(18.dp), tint = RosePaid)
                                    Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                                    Text(if (isBlocked) strings.unblockUser else strings.blockUser, color = RosePaid)
                                }
                                IconButton(
                                    onClick = { onReportCreator(creator.userId) },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(Icons.Filled.Flag, contentDescription = strings.report, tint = RosePaid)
                                }
                            }
                        }
                    }
                }
            }

            if (creatorPosts.isNotEmpty()) {
                item {
                    Text(strings.postsLabel, style = MaterialTheme.typography.titleMedium, color = glass.cyanAccent)
                }
                items(creatorPosts, key = { "cp_${it.id}" }) { post ->
                    LiquidGlassCard(onClick = { onOpenPost(post.id) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(post.title, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                            Text(
                                text = post.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            if (creatorResources.isNotEmpty()) {
                item {
                    Text(strings.resourcesLabel, style = MaterialTheme.typography.titleMedium, color = glass.cyanAccent)
                }
                items(creatorResources, key = { "cr_${it.id}" }) { res ->
                    LiquidGlassCard(onClick = { onOpenResource(res.id) }) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(if (isArabic) res.titleAr else res.title, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                            Text("${res.fileFormat} • ${res.licenseInfo}", style = MaterialTheme.typography.labelSmall, color = glass.violetAccent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavedItemsScreen(
    savedItems: List<SavedItem>,
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSignInClick: () -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    onRemoveSaved: (SavedItem) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CreovaTopBar(title = strings.savedItemsTitle, onBack = onBack)

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isSignedIn) {
                item {
                    EmptyStateGlassCard(
                        title = strings.signInRequiredTitle,
                        body = strings.signInRequiredBody,
                        icon = Icons.Filled.Bookmark,
                        ctaLabel = strings.signInWithGoogle,
                        onCtaClick = onSignInClick
                    )
                }
            } else if (savedItems.isEmpty()) {
                item {
                    EmptyStateGlassCard(
                        title = strings.emptyTitle,
                        body = strings.emptyBody,
                        icon = Icons.Filled.Bookmark
                    )
                }
            } else {
                items(savedItems, key = { "${it.targetType}_${it.targetId}" }) { item ->
                    LiquidGlassCard(
                        onClick = {
                            when (item.targetType) {
                                "tool" -> onOpenTool(item.targetId)
                                "resource" -> onOpenResource(item.targetId)
                                "post" -> onOpenPost(item.targetId)
                            }
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(item.targetType.uppercase(), style = MaterialTheme.typography.labelSmall, color = glass.cyanAccent)
                                Text(item.title, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                                if (item.subtitle.isNotBlank()) {
                                    Text(item.subtitle, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = { onRemoveSaved(item) }) {
                                Icon(Icons.Filled.BookmarkRemove, contentDescription = strings.saved, tint = RosePaid)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    notifications: List<NotificationItem>,
    isSignedIn: Boolean,
    onBack: () -> Unit,
    onSignInClick: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        CreovaTopBar(
            title = strings.notificationsTitle,
            onBack = onBack,
            actions = {
                if (isSignedIn && notifications.any { !it.seen }) {
                    TextButton(onClick = onMarkAllRead) {
                        Icon(Icons.Filled.DoneAll, contentDescription = null, tint = glass.cyanAccent, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.padding(horizontal = 2.dp))
                        Text(strings.markAllRead, color = glass.cyanAccent)
                    }
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!isSignedIn) {
                item {
                    EmptyStateGlassCard(
                        title = strings.signInRequiredTitle,
                        body = strings.signInRequiredBody,
                        icon = Icons.Filled.NotificationsNone,
                        ctaLabel = strings.signInWithGoogle,
                        onCtaClick = onSignInClick
                    )
                }
            } else if (notifications.isEmpty()) {
                item {
                    EmptyStateGlassCard(
                        title = strings.emptyTitle,
                        body = strings.emptyBody,
                        icon = Icons.Filled.NotificationsNone
                    )
                }
            } else {
                items(notifications, key = { it.id }) { notif ->
                    LiquidGlassCard(elevated = !notif.seen) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (!notif.seen) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(glass.cyanAccent)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(notif.actorName, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                                Text(notif.message, style = MaterialTheme.typography.bodyMedium, color = glass.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}
