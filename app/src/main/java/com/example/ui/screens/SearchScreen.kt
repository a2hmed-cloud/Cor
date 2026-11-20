package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.domain.model.ToolCategory
import com.example.ui.components.CreovaTopBar
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.PricingBadge
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.viewmodel.GlobalSearchResults

@Composable
fun SearchScreen(
    language: AppLanguage,
    query: String,
    recentSearches: List<String>,
    results: GlobalSearchResults,
    onQueryChange: (String) -> Unit,
    onCommitQuery: (String) -> Unit,
    onClearRecent: () -> Unit,
    onBack: () -> Unit,
    onOpenTool: (String) -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenPost: (String) -> Unit,
    onOpenCreator: (String) -> Unit,
    onSelectCategory: (ToolCategory) -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val isArabic = language == AppLanguage.AR

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        CreovaTopBar(title = strings.searchTitle, onBack = onBack)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = strings.clearAll)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("global_search_input")
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (query.isBlank()) {
                if (recentSearches.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.recentSearches, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                            TextButton(onClick = onClearRecent) {
                                Text(strings.clearAll, color = glass.cyanAccent)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recentSearches) { recent ->
                                LiquidGlassPill(
                                    text = recent,
                                    selected = false,
                                    onClick = { onQueryChange(recent) }
                                )
                            }
                        }
                    }
                }
                item {
                    Text(strings.categoryShortcuts, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        items(ToolCategory.entries) { cat ->
                            LiquidGlassPill(
                                text = if (isArabic) cat.labelAr else cat.labelEn,
                                selected = false,
                                onClick = { onSelectCategory(cat) }
                            )
                        }
                    }
                }
            } else {
                val emptyAll = results.tools.isEmpty() &&
                    results.resources.isEmpty() &&
                    results.posts.isEmpty() &&
                    results.creators.isEmpty() &&
                    results.toolCategories.isEmpty() &&
                    results.resourceCategories.isEmpty()

                if (emptyAll) {
                    item {
                        EmptyStateGlassCard(
                            title = strings.emptyTitle,
                            body = strings.emptyBody,
                            icon = Icons.Filled.Search
                        )
                    }
                } else {
                    if (results.tools.isNotEmpty()) {
                        item {
                            Text(strings.navExplore, style = MaterialTheme.typography.titleMedium, color = glass.cyanAccent)
                        }
                        items(results.tools, key = { "st_${it.id}" }) { tool ->
                            LiquidGlassCard(onClick = {
                                onCommitQuery(query)
                                onOpenTool(tool.id)
                            }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(if (isArabic) tool.nameAr else tool.name, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                                        Text(if (isArabic) tool.descriptionAr else tool.description, style = MaterialTheme.typography.bodySmall, color = glass.textSecondary, maxLines = 1)
                                    }
                                    PricingBadge(tool.pricingTier)
                                }
                            }
                        }
                    }

                    if (results.resources.isNotEmpty()) {
                        item {
                            Text(strings.navLibrary, style = MaterialTheme.typography.titleMedium, color = glass.cyanAccent)
                        }
                        items(results.resources, key = { "sr_${it.id}" }) { res ->
                            LiquidGlassCard(onClick = {
                                onCommitQuery(query)
                                onOpenResource(res.id)
                            }) {
                                Column {
                                    Text(if (isArabic) res.titleAr else res.title, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                                    Text("${res.fileFormat} • ${res.licenseInfo}", style = MaterialTheme.typography.labelSmall, color = glass.violetAccent)
                                }
                            }
                        }
                    }

                    if (results.posts.isNotEmpty()) {
                        item {
                            Text(strings.navCommunity, style = MaterialTheme.typography.titleMedium, color = glass.cyanAccent)
                        }
                        items(results.posts, key = { "sp_${it.id}" }) { post ->
                            LiquidGlassCard(onClick = {
                                onCommitQuery(query)
                                onOpenPost(post.id)
                            }) {
                                Column {
                                    Text(post.title, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                                    Text(post.authorName, style = MaterialTheme.typography.labelSmall, color = glass.textSecondary)
                                }
                            }
                        }
                    }

                    if (results.creators.isNotEmpty()) {
                        item {
                            Text(strings.popularCreators, style = MaterialTheme.typography.titleMedium, color = glass.cyanAccent)
                        }
                        items(results.creators, key = { "sc_${it.userId}" }) { creator ->
                            LiquidGlassCard(onClick = {
                                onCommitQuery(query)
                                onOpenCreator(creator.userId)
                            }) {
                                Column {
                                    Text(creator.displayName, style = MaterialTheme.typography.titleSmall, color = glass.textPrimary)
                                    Text("@${creator.username}", style = MaterialTheme.typography.labelSmall, color = glass.cyanAccent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
