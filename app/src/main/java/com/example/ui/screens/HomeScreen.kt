package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.domain.analytics.RankingStatus
import com.example.domain.analytics.TrendingEngine
import com.example.domain.model.CommunityPost
import com.example.domain.model.CreativeResource
import com.example.domain.model.CreativeTool
import com.example.domain.model.EngagementEvent
import com.example.domain.model.PublicProfile
import com.example.domain.model.ToolCategory
import com.example.domain.model.TrendingSection
import com.example.ui.components.EditorialSectionHeader
import com.example.ui.components.LiquidGlassAiOrb
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassHeroArtwork
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.PricingBadge
import com.example.ui.components.VerifiedPill
import com.example.ui.theme.LocalCreovaGlass
import java.util.Calendar

@Composable
fun HomeScreen(
    language: AppLanguage,
    tools: List<CreativeTool>,
    resources: List<CreativeResource>,
    posts: List<CommunityPost>,
    creators: List<PublicProfile>,
    events: List<EngagementEvent>,
    savedKeys: Set<String>,
    unreadNotificationsCount: Int,
    onToggleLanguage: () -> Unit,
    onOpenSearch: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAiAssistant: () -> Unit,
    onSelectToolCategory: (ToolCategory) -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenCreator: (String) -> Unit,
    onToggleSaveTool: (CreativeTool) -> Unit,
    onSeeAllTools: () -> Unit,
    onSeeAllLibrary: () -> Unit,
    onSeeAllCommunity: () -> Unit,
    contentPadding: PaddingValues
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val isArabic = language == AppLanguage.AR

    var selectedTrending by remember { mutableStateOf(TrendingSection.TODAY) }

    val dailyTool = remember(tools) {
        if (tools.isEmpty()) null else {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            tools[dayOfYear % tools.size]
        }
    }
    val dailyResource = remember(resources) {
        if (resources.isEmpty()) null else {
            val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            resources[dayOfYear % resources.size]
        }
    }

    val rankedTools = remember(tools, events, selectedTrending) {
        when (selectedTrending) {
            TrendingSection.TODAY, TrendingSection.THIS_WEEK, TrendingSection.THIS_MONTH -> {
                val window = TrendingEngine.windowDaysForSection(selectedTrending)
                tools.sortedByDescending { tool ->
                    when (val status = TrendingEngine.scoreEventsForTarget(events, "tool", tool.id, window)) {
                        is RankingStatus.Ranked -> status.score + (tool.savesCount * 0.01)
                        RankingStatus.InsufficientData -> tool.savesCount * 0.01
                    }
                }
            }
            TrendingSection.MOST_SAVED -> tools.sortedByDescending { it.savesCount }
            TrendingSection.HIGHEST_RATED -> tools.sortedByDescending { it.ratingAvg }
            TrendingSection.RECENTLY_ADDED -> tools.reversed()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = contentPadding.calculateTopPadding() + 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
            start = 16.dp,
            end = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Utility Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = glass.textPrimary
                    )
                    Text(
                        text = strings.tagline,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = glass.glassSurface,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .border(1.dp, glass.glassBorderStart, RoundedCornerShape(999.dp))
                            .clickable(onClick = onToggleLanguage)
                            .testTag("home_language_toggle")
                    ) {
                        Text(
                            text = if (isArabic) "EN" else "عربي",
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.cyanAccent,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenSearch,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(glass.glassSurface)
                            .border(1.dp, glass.glassBorderStart, CircleShape)
                            .testTag("home_search_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = strings.searchTitle,
                            tint = glass.textPrimary
                        )
                    }
                    Box {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(glass.glassSurface)
                                .border(1.dp, glass.glassBorderStart, CircleShape)
                                .testTag("home_notifications_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = strings.notificationsTitle,
                                tint = glass.textPrimary
                            )
                        }
                        if (unreadNotificationsCount > 0) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(6.dp)
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(glass.cyanAccent)
                            )
                        }
                    }
                }
            }
        }

        // Editorial Liquid Glass Hero Banner
        item {
            LiquidGlassCard(
                elevated = true,
                cornerRadius = 28.dp,
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(modifier = Modifier.fillMaxWidth()) {
                    LiquidGlassHeroArtwork(
                        modifier = Modifier
                            .matchParentSize()
                    )
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = strings.homeHeroLabel,
                            style = MaterialTheme.typography.labelMedium,
                            color = glass.cyanAccent
                        )
                        Text(
                            text = strings.homeHeroTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            color = glass.textPrimary
                        )
                        Text(
                            text = strings.homeHeroBody,
                            style = MaterialTheme.typography.bodyMedium,
                            color = glass.textSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            LiquidGlassPill(
                                text = strings.navExplore,
                                selected = true,
                                onClick = onSeeAllTools
                            )
                            LiquidGlassPill(
                                text = strings.navLibrary,
                                selected = false,
                                onClick = onSeeAllLibrary
                            )
                        }
                    }
                }
            }
        }

        // CREOVA AI Spotlight Card
        item {
            LiquidGlassCard(
                onClick = onOpenAiAssistant,
                elevated = true,
                modifier = Modifier.testTag("home_ai_card"),
                contentPadding = PaddingValues(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.size(56.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LiquidGlassAiOrb(modifier = Modifier.fillMaxSize())
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = strings.aiTitle,
                            tint = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = strings.aiBannerTitle,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Text(
                            text = strings.aiBannerSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = glass.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Category Shortcuts
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorialSectionHeader(title = strings.categoryShortcuts)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ToolCategory.entries) { cat ->
                        LiquidGlassPill(
                            text = if (isArabic) cat.labelAr else cat.labelEn,
                            selected = false,
                            onClick = { onSelectToolCategory(cat) }
                        )
                    }
                }
            }
        }

        // Daily Discovery
        if (dailyTool != null || dailyResource != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    EditorialSectionHeader(title = strings.dailyDiscovery)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (dailyTool != null) {
                            LiquidGlassCard(
                                onClick = { onOpenTool(dailyTool.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    VerifiedPill()
                                    Text(
                                        text = if (isArabic) dailyTool.nameAr else dailyTool.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = glass.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isArabic) dailyTool.descriptionAr else dailyTool.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    PricingBadge(dailyTool.pricingTier)
                                }
                            }
                        }
                        if (dailyResource != null) {
                            LiquidGlassCard(
                                onClick = { onOpenResource(dailyResource.id) },
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(999.dp),
                                        color = glass.violetAccent.copy(alpha = 0.18f)
                                    ) {
                                        Text(
                                            text = dailyResource.fileFormat,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = glass.violetAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Text(
                                        text = if (isArabic) dailyResource.titleAr else dailyResource.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = glass.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isArabic) dailyResource.descriptionAr else dailyResource.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = glass.textSecondary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = dailyResource.licenseInfo,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = glass.cyanAccent,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Trending Sections
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                EditorialSectionHeader(
                    title = strings.trendingNow,
                    subtitle = strings.creovaEngagementNote,
                    actionLabel = strings.navExplore,
                    onActionClick = onSeeAllTools
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val tabs = listOf(
                        TrendingSection.TODAY to strings.trendingToday,
                        TrendingSection.THIS_WEEK to strings.trendingWeek,
                        TrendingSection.THIS_MONTH to strings.trendingMonth,
                        TrendingSection.MOST_SAVED to strings.mostSaved,
                        TrendingSection.HIGHEST_RATED to strings.highestRated,
                        TrendingSection.RECENTLY_ADDED to strings.recentlyAdded
                    )
                    items(tabs) { (sec, label) ->
                        LiquidGlassPill(
                            text = label,
                            selected = selectedTrending == sec,
                            leadingIcon = Icons.AutoMirrored.Filled.TrendingUp,
                            onClick = { selectedTrending = sec }
                        )
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(rankedTools.take(6), key = { it.id }) { tool ->
                    val status = TrendingEngine.scoreEventsForTarget(
                        events = events,
                        targetType = "tool",
                        targetId = tool.id,
                        windowDays = TrendingEngine.windowDaysForSection(selectedTrending)
                    )
                    val isSaved = "tool_${tool.id}" in savedKeys
                    LiquidGlassCard(
                        onClick = { onOpenTool(tool.id) },
                        modifier = Modifier.width(240.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PricingBadge(tool.pricingTier)
                                IconButton(
                                    onClick = { onToggleSaveTool(tool) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = strings.save,
                                        tint = if (isSaved) glass.cyanAccent else glass.textSecondary
                                    )
                                }
                            }
                            Text(
                                text = if (isArabic) tool.nameAr else tool.name,
                                style = MaterialTheme.typography.titleMedium,
                                color = glass.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isArabic) tool.descriptionAr else tool.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = glass.cyanAccent,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = "${tool.ratingAvg}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = glass.textPrimary
                                    )
                                }
                                Text(
                                    text = when (status) {
                                        is RankingStatus.Ranked -> "${status.eventCount} • ${strings.trendingToday}"
                                        RankingStatus.InsufficientData -> strings.newBadge
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.violetAccent
                                )
                            }
                        }
                    }
                }
            }
        }

        // Popular Resources Preview
        item {
            EditorialSectionHeader(
                title = strings.libraryTitle,
                actionLabel = strings.navLibrary,
                onActionClick = onSeeAllLibrary
            )
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(resources.take(6), key = { it.id }) { res ->
                    LiquidGlassCard(
                        onClick = { onOpenResource(res.id) },
                        modifier = Modifier.width(230.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = res.fileFormat,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.cyanAccent
                            )
                            Text(
                                text = if (isArabic) res.titleAr else res.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (isArabic) res.descriptionAr else res.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = glass.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = res.creatorName,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.violetAccent
                            )
                        }
                    }
                }
            }
        }

        // Popular Creators
        item {
            EditorialSectionHeader(title = strings.popularCreators)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(creators.take(8), key = { it.userId }) { creator ->
                    LiquidGlassCard(
                        onClick = { onOpenCreator(creator.userId) },
                        modifier = Modifier.width(170.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(glass.violetAccent, glass.cyanAccent))
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = creator.displayName.take(1),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = androidx.compose.ui.graphics.Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = creator.displayName,
                                style = MaterialTheme.typography.titleSmall,
                                color = glass.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = creator.specialty.ifBlank { "@${creator.username}" },
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.cyanAccent,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // Latest Community Posts
        item {
            EditorialSectionHeader(
                title = strings.latestCommunity,
                actionLabel = strings.navCommunity,
                onActionClick = onSeeAllCommunity
            )
        }
        items(posts.take(3), key = { it.id }) { post ->
            LiquidGlassCard(onClick = { onOpenPost(post.id) }) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = post.authorName,
                            style = MaterialTheme.typography.labelLarge,
                            color = glass.cyanAccent
                        )
                        Text(
                            text = if (isArabic) post.resolvedType.labelAr else post.resolvedType.labelEn,
                            style = MaterialTheme.typography.labelSmall,
                            color = glass.violetAccent
                        )
                    }
                    Text(
                        text = post.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = glass.textPrimary
                    )
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
}
