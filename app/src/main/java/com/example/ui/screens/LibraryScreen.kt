package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.CollectionsBookmark
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.domain.model.CreativeResource
import com.example.domain.model.ResourceCategory
import com.example.domain.model.ResourceSort
import com.example.ui.components.EmptyStateGlassCard
import com.example.ui.components.LiquidGlassCard
import com.example.ui.components.LiquidGlassPill
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid

@Composable
fun LibraryScreen(
    language: AppLanguage,
    resources: List<CreativeResource>,
    savedKeys: Set<String>,
    likedKeys: Set<String>,
    onOpenResource: (String) -> Unit,
    onToggleSaveResource: (CreativeResource) -> Unit,
    onToggleLikeResource: (CreativeResource) -> Unit,
    onOpenSubmitResource: () -> Unit,
    contentPadding: PaddingValues
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    val isArabic = language == AppLanguage.AR

    var selectedCategory by remember { mutableStateOf<ResourceCategory?>(null) }
    var selectedSort by remember { mutableStateOf(ResourceSort.POPULAR) }
    var searchQuery by remember { mutableStateOf("") }
    var pageLimit by remember { mutableIntStateOf(10) }

    val filteredAndSorted = remember(resources, selectedCategory, selectedSort, searchQuery) {
        val q = searchQuery.trim().lowercase()
        val list = resources.filter { res ->
            val catOk = selectedCategory == null || res.category == selectedCategory!!.id
            val qOk = q.isEmpty() ||
                res.title.lowercase().contains(q) ||
                res.titleAr.contains(searchQuery.trim()) ||
                res.description.lowercase().contains(q) ||
                res.tags.any { it.lowercase().contains(q) }
            catOk && qOk
        }
        when (selectedSort) {
            ResourceSort.POPULAR -> list.sortedByDescending { it.likesCount + it.savesCount }
            ResourceSort.NEWEST -> list.sortedByDescending { it.createdAt?.seconds ?: 0L }
            ResourceSort.MOST_SAVED -> list.sortedByDescending { it.savesCount }
        }
    }

    val visibleItems = remember(filteredAndSorted, pageLimit) {
        filteredAndSorted.take(pageLimit)
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = strings.libraryTitle,
                        style = MaterialTheme.typography.headlineMedium,
                        color = glass.textPrimary
                    )
                    Text(
                        text = strings.librarySubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = glass.textSecondary
                    )
                }
                Button(
                    onClick = onOpenSubmitResource,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = glass.violetAccent,
                        contentColor = androidx.compose.ui.graphics.Color.White
                    ),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("submit_resource_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Text(
                        text = strings.submitResource,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = {
                    searchQuery = it
                    pageLimit = 10
                },
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
                    .testTag("library_search_input")
            )
        }

        // All 19 Resource Categories
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    LiquidGlassPill(
                        text = strings.allFilter,
                        selected = selectedCategory == null,
                        onClick = {
                            selectedCategory = null
                            pageLimit = 10
                        }
                    )
                }
                items(ResourceCategory.entries) { cat ->
                    LiquidGlassPill(
                        text = if (isArabic) cat.labelAr else cat.labelEn,
                        selected = selectedCategory == cat,
                        onClick = {
                            selectedCategory = if (selectedCategory == cat) null else cat
                            pageLimit = 10
                        }
                    )
                }
            }
        }

        // Sort Pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sorts = listOf(
                    ResourceSort.POPULAR to strings.sortPopular,
                    ResourceSort.NEWEST to strings.sortNewest,
                    ResourceSort.MOST_SAVED to strings.sortMostSaved
                )
                items(sorts) { (sort, label) ->
                    LiquidGlassPill(
                        text = label,
                        selected = selectedSort == sort,
                        onClick = { selectedSort = sort }
                    )
                }
            }
        }

        if (visibleItems.isEmpty()) {
            item {
                EmptyStateGlassCard(
                    title = strings.emptyTitle,
                    body = strings.emptyBody,
                    icon = Icons.Filled.CollectionsBookmark,
                    ctaLabel = strings.allFilter,
                    onCtaClick = {
                        selectedCategory = null
                        searchQuery = ""
                    }
                )
            }
        } else {
            items(visibleItems, key = { it.id }) { res ->
                val isSaved = "resource_${res.id}" in savedKeys
                val isLiked = "resource_${res.id}" in likedKeys
                val catObj = ResourceCategory.fromId(res.category)

                LiquidGlassCard(
                    onClick = { onOpenResource(res.id) },
                    modifier = Modifier.testTag("resource_card_${res.id}")
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = glass.cyanAccent.copy(alpha = 0.16f)
                            ) {
                                Text(
                                    text = "${if (isArabic) catObj.labelAr else catObj.labelEn} • ${res.fileFormat}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.cyanAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Text(
                                text = res.fileSizeLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.textSecondary
                            )
                        }

                        Text(
                            text = if (isArabic) res.titleAr else res.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = glass.textPrimary
                        )
                        Text(
                            text = if (isArabic) res.descriptionAr else res.description,
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
                            Text(
                                text = res.licenseInfo,
                                style = MaterialTheme.typography.labelSmall,
                                color = glass.violetAccent,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { onToggleLikeResource(res) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                        contentDescription = null,
                                        tint = if (isLiked) RosePaid else glass.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "${res.likesCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = glass.textSecondary
                                )
                                IconButton(
                                    onClick = { onToggleSaveResource(res) },
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isSaved) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                                        contentDescription = strings.save,
                                        tint = if (isSaved) glass.cyanAccent else glass.textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (visibleItems.size < filteredAndSorted.size) {
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
}
