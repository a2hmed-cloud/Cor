package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.domain.model.CreativeTool
import com.example.domain.model.PricingTier
import com.example.domain.model.ToolCategory
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.components.PricingBadge
import com.example.ui.components.VerifiedPill
import com.example.ui.theme.LocalCreovaGlass

@Composable
fun ExploreToolsScreen(
    language: AppLanguage,
    tools: List<CreativeTool>,
    savedKeys: Set<String>,
    initialCategory: ToolCategory?,
    onOpenTool: (String) -> Unit,
    onToggleSaveTool: (CreativeTool) -> Unit,
    contentPadding: PaddingValues
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val isArabic = language == AppLanguage.AR

    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var selectedPricing by remember { mutableStateOf<PricingTier?>(null) }
    var localFilter by remember { mutableStateOf("") }

    val filtered = remember(tools, selectedCategory, selectedPricing, localFilter) {
        val q = localFilter.trim().lowercase()
        tools.filter { tool ->
            val catOk = selectedCategory == null || selectedCategory!!.id in tool.categories
            val priceOk = selectedPricing == null || tool.pricingTier == selectedPricing
            val queryOk = q.isEmpty() ||
                tool.name.lowercase().contains(q) ||
                tool.nameAr.contains(localFilter.trim()) ||
                tool.description.lowercase().contains(q) ||
                tool.descriptionAr.contains(localFilter.trim())
            catOk && priceOk && queryOk
        }
    }

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
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = strings.toolsTitle,
                    style = MaterialTheme.typography.headlineMedium,
                    color = glass.textPrimary
                )
                Text(
                    text = strings.toolsSubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = glass.textSecondary
                )
            }
        }

        item {
            OutlinedTextField(
                value = localFilter,
                onValueChange = { localFilter = it },
                placeholder = { Text(strings.searchPlaceholder) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = glass.cyanAccent,
                    unfocusedBorderColor = glass.glassBorderStart,
                    focusedContainerColor = glass.glassSurface,
                    unfocusedContainerColor = glass.glassSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("tools_filter_input")
            )
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    LiquidGlassPill(
                        text = strings.allFilter,
                        selected = selectedCategory == null,
                        onClick = { selectedCategory = null }
                    )
                }
                items(ToolCategory.entries) { cat ->
                    LiquidGlassPill(
                        text = if (isArabic) cat.labelAr else cat.labelEn,
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat }
                    )
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val pricingChoices = listOf(
                    PricingTier.FREE to strings.pricingFree,
                    PricingTier.FREEMIUM to strings.pricingFreemium,
                    PricingTier.PAID to strings.pricingPaid,
                    PricingTier.OPEN_SOURCE to strings.pricingOpenSource
                )
                items(pricingChoices) { (tier, label) ->
                    LiquidGlassPill(
                        text = label,
                        selected = selectedPricing == tier,
                        onClick = { selectedPricing = if (selectedPricing == tier) null else tier }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                EmptyStateGlassCard(
                    title = strings.emptyTitle,
                    body = strings.emptyBody,
                    icon = Icons.Filled.Explore,
                    ctaLabel = strings.allFilter,
                    onCtaClick = {
                        selectedCategory = null
                        selectedPricing = null
                        localFilter = ""
                    }
                )
            }
        } else {
            items(filtered, key = { it.id }) { tool ->
                val isSaved = "tool_${tool.id}" in savedKeys
                LiquidGlassCard(
                    onClick = { onOpenTool(tool.id) },
                    modifier = Modifier.testTag("tool_card_${tool.id}")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = if (isArabic) tool.nameAr else tool.name,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = glass.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (tool.verified) {
                                    VerifiedPill()
                                }
                            }
                            IconButton(
                                onClick = { onToggleSaveTool(tool) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("save_tool_${tool.id}")
                            ) {
                                Icon(
                                    imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = strings.save,
                                    tint = if (isSaved) glass.cyanAccent else glass.textSecondary
                                )
                            }
                        }

                        Text(
                            text = if (isArabic) tool.descriptionAr else tool.description,
                            style = MaterialTheme.typography.bodyMedium,
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
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                PricingBadge(tool.pricingTier)
                                Text(
                                    text = tool.platforms.take(3).joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Star,
                                    contentDescription = null,
                                    tint = glass.cyanAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = "${tool.ratingAvg} (${tool.savesCount})",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = glass.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
