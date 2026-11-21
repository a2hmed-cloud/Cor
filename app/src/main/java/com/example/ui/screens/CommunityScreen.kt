package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.domain.model.CommunityFeedTab
import com.example.domain.model.CommunityPost
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid
import android.content.Intent
import android.util.Base64

@Composable
fun CommunityScreen(
    language: AppLanguage,
    posts: List<CommunityPost>,
    followingIds: Set<String>,
    likedKeys: Set<String>,
    savedKeys: Set<String>,
    currentUserId: String?,
    onOpenPost: (String) -> Unit,
    onOpenCreator: (String) -> Unit,
    onCreatePost: () -> Unit,
    onToggleLikePost: (CommunityPost) -> Unit,
    onToggleSavePost: (CommunityPost) -> Unit,
    onSharePost: (CommunityPost) -> Unit,
    onEditPost: (CommunityPost) -> Unit,
    onDeletePost: (CommunityPost) -> Unit,
    onReportPost: (CommunityPost) -> Unit,
    onBlockAuthor: (String) -> Unit,
    contentPadding: PaddingValues
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val context = LocalContext.current
    val isArabic = language == AppLanguage.AR

    var selectedTab by remember { mutableStateOf(CommunityFeedTab.FOR_YOU) }
    var pageLimit by remember { mutableIntStateOf(10) }
    var pendingDeletePost by remember { mutableStateOf<CommunityPost?>(null) }

    val feedPosts = remember(posts, selectedTab, followingIds) {
        when (selectedTab) {
            CommunityFeedTab.FOR_YOU -> posts.sortedByDescending { it.likesCount * 2 + it.savesCount * 3 + it.commentsCount * 2 }
            CommunityFeedTab.FOLLOWING -> posts.filter { it.authorId in followingIds }
            CommunityFeedTab.TRENDING -> posts.sortedByDescending { it.likesCount + it.sharesCount * 2 }
            CommunityFeedTab.LATEST -> posts.sortedByDescending { it.createdAt?.seconds ?: 0L }
        }
    }
    val visiblePosts = remember(feedPosts, pageLimit) { feedPosts.take(pageLimit) }

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.communityTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = glass.textPrimary
                )
                Button(
                    onClick = onCreatePost,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = glass.violetAccent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("create_post_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = strings.createPost,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val tabs = listOf(
                    CommunityFeedTab.FOR_YOU to strings.feedForYou,
                    CommunityFeedTab.FOLLOWING to strings.feedFollowing,
                    CommunityFeedTab.TRENDING to strings.feedTrending,
                    CommunityFeedTab.LATEST to strings.feedLatest
                )
                items(tabs) { (tab, label) ->
                    LiquidGlassPill(
                        text = label,
                        selected = selectedTab == tab,
                        onClick = {
                            selectedTab = tab
                            pageLimit = 10
                        }
                    )
                }
            }
        }

        if (visiblePosts.isEmpty()) {
            item {
                EmptyStateGlassCard(
                    title = strings.emptyTitle,
                    body = strings.emptyBody,
                    icon = Icons.Filled.Forum,
                    ctaLabel = strings.createPost,
                    onCtaClick = onCreatePost
                )
            }
        } else {
            items(visiblePosts, key = { it.id }) { post ->
                val isLiked = "post_${post.id}" in likedKeys
                val isSaved = "post_${post.id}" in savedKeys
                var menuOpen by remember { mutableStateOf(false) }

                LiquidGlassCard(
                    onClick = { onOpenPost(post.id) },
                    modifier = Modifier.testTag("post_card_${post.id}")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenCreator(post.authorId) }
                            ) {
                                Text(
                                    text = post.authorName,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = glass.textPrimary
                                )
                                Text(
                                    text = "@${post.authorUsername}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.cyanAccent
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = glass.violetAccent.copy(alpha = 0.16f)
                                ) {
                                    Text(
                                        text = if (isArabic) post.resolvedType.labelAr else post.resolvedType.labelEn,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.violetAccent,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(onClick = { menuOpen = true }) {
                                    Icon(
                                        imageVector = Icons.Filled.MoreVert,
                                        contentDescription = "More",
                                        tint = glass.textSecondary
                                    )
                                    DropdownMenu(
                                        expanded = menuOpen,
                                        onDismissRequest = { menuOpen = false }
                                    ) {
                                        if (currentUserId == post.authorId) {
                                            DropdownMenuItem(
                                                text = { Text(if (isArabic) "تعديل المنشور" else "Edit post") },
                                                onClick = {
                                                    menuOpen = false
                                                    onEditPost(post)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(if (isArabic) "حذف المنشور" else "Delete post") },
                                                onClick = {
                                                    menuOpen = false
                                                    pendingDeletePost = post
                                                }
                                            )
                                        } else {
                                            DropdownMenuItem(
                                                text = { Text(strings.report) },
                                                onClick = {
                                                    menuOpen = false
                                                    onReportPost(post)
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text(strings.blockUser) },
                                                onClick = {
                                                    menuOpen = false
                                                    onBlockAuthor(post.authorId)
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = post.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Text(
                            text = post.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (post.mediaDataUrl.isNotBlank()) {
                            AsyncImage(
                                model = remember(post.mediaDataUrl) { post.mediaDataUrl.let { value -> if (value.startsWith("data:image/")) Base64.decode(value.substringAfter(","), Base64.DEFAULT) else value } },
                                contentDescription = post.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onToggleLikePost(post) }) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = "Like",
                                        tint = if (isLiked) RosePaid else glass.textSecondary
                                    )
                                }
                                Text("${post.likesCount}", style = MaterialTheme.typography.labelMedium, color = glass.textSecondary)

                                Spacer(modifier = Modifier.padding(horizontal = 6.dp))

                                Icon(
                                    imageVector = Icons.Filled.ChatBubbleOutline,
                                    contentDescription = strings.commentsTitle,
                                    tint = glass.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = " ${post.commentsCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = glass.textSecondary
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = {
                                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${post.title}\n${post.description}\ncreova://post/${post.id}")
                                    }
                                    context.startActivity(Intent.createChooser(sendIntent, strings.share))
                                    onSharePost(post)
                                }) {
                                    Icon(
                                        imageVector = Icons.Filled.Share,
                                        contentDescription = strings.share,
                                        tint = glass.textSecondary
                                    )
                                }
                                IconButton(onClick = { onToggleSavePost(post) }) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = strings.save,
                                        tint = if (isSaved) glass.cyanAccent else glass.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (visiblePosts.size < feedPosts.size) {
                item {
                    OutlinedButton(
                        onClick = { pageLimit += 10 },
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(strings.loadMore, color = glass.cyanAccent)
                    }
                }
            }
        }
    }

    pendingDeletePost?.let { post ->
        AlertDialog(
            onDismissRequest = { pendingDeletePost = null },
            title = { Text(if (isArabic) "حذف المنشور؟" else "Delete post?") },
            text = { Text(if (isArabic) "مش هتقدر ترجع المنشور بعد حذفه." else "This post will be permanently removed.") },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = {
                    pendingDeletePost = null
                    onDeletePost(post)
                }) { Text(if (isArabic) "حذف" else "Delete", color = RosePaid) }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { pendingDeletePost = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
