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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.core.security.SafeUrlLauncher
import com.example.domain.model.CommunityPost
import com.example.domain.model.PostComment
import com.example.ui.components.CreovaTopBar
import com.example.ui.components.LiquidGlassCard
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid

@Composable
fun PostDetailScreen(
    language: AppLanguage,
    post: CommunityPost?,
    comments: List<PostComment>,
    isLiked: Boolean,
    isSaved: Boolean,
    isFollowingAuthor: Boolean,
    currentUserId: String?,
    onBack: () -> Unit,
    onOpenCreator: (String) -> Unit,
    onToggleLike: (CommunityPost) -> Unit,
    onToggleSave: (CommunityPost) -> Unit,
    onToggleFollow: (String) -> Unit,
    onSendComment: (postId: String, postAuthorId: String?, body: String, parentCommentId: String?) -> Unit,
    onShowError: (String) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val context = LocalContext.current

    var commentDraft by remember { mutableStateOf("") }
    var replyTarget by remember { mutableStateOf<PostComment?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        CreovaTopBar(
            title = post?.title ?: strings.communityTitle,
            onBack = onBack,
            actions = {
                if (post != null) {
                    IconButton(onClick = { onToggleLike(post) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = null,
                            tint = if (isLiked) RosePaid else glass.textPrimary
                        )
                    }
                    IconButton(onClick = { onToggleSave(post) }) {
                        Icon(
                            imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = strings.save,
                            tint = if (isSaved) glass.cyanAccent else glass.textPrimary
                        )
                    }
                }
            }
        )

        if (post == null) return@Column

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LiquidGlassCard(elevated = true) {
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
                                Text(post.authorName, style = MaterialTheme.typography.titleMedium, color = glass.textPrimary)
                                Text("@${post.authorUsername}", style = MaterialTheme.typography.labelSmall, color = glass.cyanAccent)
                            }
                            if (currentUserId != post.authorId) {
                                TextButton(onClick = { onToggleFollow(post.authorId) }) {
                                    Text(
                                        text = if (isFollowingAuthor) strings.following else strings.follow,
                                        color = glass.cyanAccent
                                    )
                                }
                            }
                        }

                        Text(post.title, style = MaterialTheme.typography.headlineSmall, color = glass.textPrimary)
                        Text(post.description, style = MaterialTheme.typography.bodyLarge, color = glass.textSecondary)

                        if (post.mediaDataUrl.isNotBlank()) {
                            AsyncImage(
                                model = post.mediaDataUrl,
                                contentDescription = post.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(220.dp)
                                    .clip(RoundedCornerShape(16.dp))
                            )
                        }

                        if (post.linkedUrl.isNotBlank()) {
                            Button(
                                onClick = {
                                    val ok = SafeUrlLauncher.openSafeCustomTab(
                                        context,
                                        post.linkedUrl,
                                        glass.canvas.toArgb()
                                    )
                                    if (!ok) onShowError(strings.urlBlockedError)
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Filled.OpenInBrowser, contentDescription = null)
                                Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                                Text(strings.openOfficialSite)
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "${strings.commentsTitle} (${comments.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = glass.textPrimary
                )
            }

            val topComments = comments.filter { it.parentCommentId.isBlank() }
            val repliesMap = comments.filter { it.parentCommentId.isNotBlank() }.groupBy { it.parentCommentId }

            items(topComments, key = { it.id }) { comment ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidGlassCard(contentPadding = PaddingValues(12.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(comment.authorName, style = MaterialTheme.typography.titleSmall, color = glass.cyanAccent)
                                Text(
                                    text = strings.replyLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.violetAccent,
                                    modifier = Modifier
                                        .clickable { replyTarget = comment }
                                        .padding(4.dp)
                                )
                            }
                            Text(comment.body, style = MaterialTheme.typography.bodyMedium, color = glass.textPrimary)
                        }
                    }

                    repliesMap[comment.id].orEmpty().forEach { reply ->
                        LiquidGlassCard(
                            modifier = Modifier.padding(start = 20.dp),
                            contentPadding = PaddingValues(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(reply.authorName, style = MaterialTheme.typography.labelLarge, color = glass.violetAccent)
                                Text(reply.body, style = MaterialTheme.typography.bodySmall, color = glass.textPrimary)
                            }
                        }
                    }
                }
            }
        }

        // Comment Composer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (replyTarget != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${strings.replyingTo}: ${replyTarget!!.authorName}",
                        style = MaterialTheme.typography.labelSmall,
                        color = glass.cyanAccent
                    )
                    IconButton(onClick = { replyTarget = null }) {
                        Icon(Icons.Filled.Close, contentDescription = strings.cancelReply, tint = glass.textSecondary)
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = commentDraft,
                    onValueChange = { commentDraft = it },
                    placeholder = { Text(strings.writeCommentHint) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        if (commentDraft.isNotBlank()) {
                            onSendComment(post.id, post.authorId, commentDraft, replyTarget?.id)
                            commentDraft = ""
                            replyTarget = null
                        }
                    }
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
}
