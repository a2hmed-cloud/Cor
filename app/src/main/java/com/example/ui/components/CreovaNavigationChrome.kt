package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalCreovaStrings
import com.example.ui.theme.LocalCreovaGlass

enum class TopTabRoute(val route: String) {
    HOME("home"),
    EXPLORE("explore"),
    LIBRARY("library"),
    COMMUNITY("community"),
    PROFILE("profile")
}

private data class NavTabMeta(
    val tab: TopTabRoute,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@Composable
private fun rememberNavTabs(): List<NavTabMeta> {
    val s = LocalCreovaStrings.current
    return listOf(
        NavTabMeta(TopTabRoute.HOME, s.navHome, Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
        NavTabMeta(TopTabRoute.EXPLORE, s.navExplore, Icons.Filled.Explore, Icons.Outlined.Explore, "nav_explore"),
        NavTabMeta(TopTabRoute.LIBRARY, s.navLibrary, Icons.Filled.CollectionsBookmark, Icons.Outlined.CollectionsBookmark, "nav_library"),
        NavTabMeta(TopTabRoute.COMMUNITY, s.navCommunity, Icons.Filled.Forum, Icons.Outlined.Forum, "nav_community"),
        NavTabMeta(TopTabRoute.PROFILE, s.navProfile, Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
    )
}

@Composable
fun LiquidGlassBottomBar(
    currentRoute: String?,
    onSelectTab: (TopTabRoute) -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalCreovaGlass.current
    val tabs = rememberNavTabs()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.verticalGradient(
                        listOf(
                            glass.glassElevated.copy(alpha = 0.94f),
                            glass.glassSurface.copy(alpha = 0.92f)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(listOf(glass.glassBorderStart, glass.glassBorderEnd)),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { item ->
                val selected = currentRoute == item.tab.route
                val activeBg = if (selected) {
                    Brush.horizontalGradient(
                        listOf(
                            glass.violetAccent.copy(alpha = if (glass.isDark) 0.30f else 0.16f),
                            glass.cyanAccent.copy(alpha = if (glass.isDark) 0.24f else 0.14f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(activeBg)
                        .testTag(item.testTag)
                        .clickable { onSelectTab(item.tab) }
                        .padding(vertical = 6.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label,
                        tint = if (selected) glass.cyanAccent else glass.textSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selected) glass.textPrimary else glass.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun LiquidGlassNavRail(
    currentRoute: String?,
    onSelectTab: (TopTabRoute) -> Unit,
    onOpenAi: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val tabs = rememberNavTabs()
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(92.dp)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(vertical = 16.dp, horizontal = 8.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(glass.glassSurface)
            .border(1.dp, glass.glassBorderStart, RoundedCornerShape(28.dp))
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tabs.forEach { item ->
            val selected = currentRoute == item.tab.route
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
                    .defaultMinSize(minHeight = 52.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (selected) glass.violetAccent.copy(alpha = 0.24f) else Color.Transparent
                    )
                    .testTag(item.testTag)
                    .clickable { onSelectTab(item.tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                    contentDescription = item.label,
                    tint = if (selected) glass.cyanAccent else glass.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) glass.textPrimary else glass.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp)
                .defaultMinSize(minHeight = 52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            glass.violetAccent.copy(alpha = 0.35f),
                            glass.cyanAccent.copy(alpha = 0.30f)
                        )
                    )
                )
                .clickable(onClick = onOpenAi)
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = strings.aiTitle,
                tint = glass.cyanAccent,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = "AI",
                style = MaterialTheme.typography.labelSmall,
                color = glass.textPrimary
            )
        }
    }
}
