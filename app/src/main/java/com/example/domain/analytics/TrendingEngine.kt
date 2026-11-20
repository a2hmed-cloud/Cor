package com.example.domain.analytics

import com.example.domain.model.EngagementEvent
import com.example.domain.model.TrendingSection
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class RankingWeights(
    val toolOpen: Double = 1.0,
    val resourceView: Double = 1.0,
    val like: Double = 2.5,
    val save: Double = 3.5,
    val comment: Double = 4.0,
    val share: Double = 4.5
)

sealed interface RankingStatus {
    data class Ranked(val score: Double, val eventCount: Int) : RankingStatus
    data object InsufficientData : RankingStatus
}

object TrendingEngine {

    const val MIN_EVENTS_FOR_RANKING = 2

    fun currentUtcDayKey(nowMillis: Long = System.currentTimeMillis()): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return formatter.format(Date(nowMillis))
    }

    fun deterministicEventId(
        userId: String,
        targetType: String,
        targetId: String,
        eventType: String,
        dayKey: String
    ): String = "${userId}_${targetType}_${targetId}_${eventType}_${dayKey}"

    fun scoreEventsForTarget(
        events: List<EngagementEvent>,
        targetType: String,
        targetId: String,
        windowDays: Int,
        nowMillis: Long = System.currentTimeMillis(),
        weights: RankingWeights = RankingWeights()
    ): RankingStatus {
        val cutoffMillis = nowMillis - windowDays.toLong() * 86_400_000L
        val matching = events.filter { ev ->
            ev.validEvent &&
                ev.targetType == targetType &&
                ev.targetId == targetId &&
                (ev.createdAt?.toDate()?.time ?: nowMillis) >= cutoffMillis
        }
        if (matching.size < MIN_EVENTS_FOR_RANKING) {
            return RankingStatus.InsufficientData
        }
        val score = matching.sumOf { ev ->
            when (ev.eventType) {
                "tool_open" -> weights.toolOpen
                "resource_view" -> weights.resourceView
                "like" -> weights.like
                "save" -> weights.save
                "comment" -> weights.comment
                "share" -> weights.share
                else -> 0.0
            }
        }
        return RankingStatus.Ranked(score = score, eventCount = matching.size)
    }

    fun windowDaysForSection(section: TrendingSection): Int = when (section) {
        TrendingSection.TODAY -> 1
        TrendingSection.THIS_WEEK -> 7
        TrendingSection.THIS_MONTH -> 30
        else -> 30
    }
}
