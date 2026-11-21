package com.example.domain.analytics

import com.example.domain.model.CommunityPost
import kotlin.math.ln

/**
 * Lightweight, explainable feed ranking. It uses only public post metadata and
 * aggregate engagement; no personal data is sent to an external service.
 */
object FeedRankingEngine {
    fun rankForYou(
        posts: List<CommunityPost>,
        specialty: String,
        followingIds: Set<String>,
        nowMillis: Long = System.currentTimeMillis()
    ): List<CommunityPost> {
        val interest = normalize(specialty)
        return posts.sortedByDescending { post ->
            val category = normalize(post.category)
            val tags = post.tags.map(::normalize)
            val fieldMatch = interest.isNotBlank() &&
                (category == interest || category.contains(interest) || interest.contains(category) ||
                    tags.any { it == interest || it.contains(interest) || interest.contains(it) })
            val ageHours = ((nowMillis - (post.createdAt?.toDate()?.time ?: nowMillis)).coerceAtLeast(0L) / 3_600_000.0)
            val freshness = 24.0 / (24.0 + ageHours)
            val engagement = ln(
                1.0 + post.likesCount * 1.0 + post.commentsCount * 2.2 +
                    post.savesCount * 2.8 + post.sharesCount * 3.2
            )
            val relationship = if (post.authorId in followingIds) 1.4 else 0.0
            val relevance = if (fieldMatch) 5.0 else 0.0
            relevance + relationship + engagement * 1.5 + freshness * 2.0
        }
    }

    fun rankTrending(posts: List<CommunityPost>): List<CommunityPost> =
        posts.sortedByDescending { post ->
            val ageHours = ((System.currentTimeMillis() -
                (post.createdAt?.toDate()?.time ?: System.currentTimeMillis())).coerceAtLeast(0L) / 3_600_000.0)
            val engagement = post.likesCount + post.commentsCount * 2.0 +
                post.savesCount * 2.5 + post.sharesCount * 3.0
            engagement / (1.0 + ageHours / 18.0)
        }

    private fun normalize(value: String): String =
        value.trim().lowercase().replace("_", " ").replace("-", " ")
}