package com.example

import com.example.domain.analytics.RankingStatus
import com.example.domain.analytics.TrendingEngine
import com.example.domain.model.EngagementEvent
import com.example.domain.model.TrendingSection
import com.google.firebase.Timestamp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class TrendingEngineTest {

    @Test
    fun `returns InsufficientData when fewer than minimum valid events exist`() {
        val now = 1_760_000_000_000L
        val singleEvent = listOf(
            EngagementEvent(
                id = "e1",
                userId = "u1",
                targetType = "tool",
                targetId = "tool_capcut",
                eventType = "tool_open",
                validEvent = true,
                createdAt = Timestamp(Date(now - 60_000L))
            )
        )
        val status = TrendingEngine.scoreEventsForTarget(
            events = singleEvent,
            targetType = "tool",
            targetId = "tool_capcut",
            windowDays = 1,
            nowMillis = now
        )
        assertEquals(RankingStatus.InsufficientData, status)
    }

    @Test
    fun `scores weighted valid events within window and ignores stale or invalid events`() {
        val now = 1_760_000_000_000L
        val events = listOf(
            EngagementEvent(
                id = "e1",
                userId = "u1",
                targetType = "tool",
                targetId = "tool_figma",
                eventType = "save", // weight 3.5
                validEvent = true,
                createdAt = Timestamp(Date(now - 2 * 3_600_000L))
            ),
            EngagementEvent(
                id = "e2",
                userId = "u2",
                targetType = "tool",
                targetId = "tool_figma",
                eventType = "share", // weight 4.5
                validEvent = true,
                createdAt = Timestamp(Date(now - 3 * 3_600_000L))
            ),
            EngagementEvent(
                id = "e_invalid",
                userId = "u3",
                targetType = "tool",
                targetId = "tool_figma",
                eventType = "share",
                validEvent = false,
                createdAt = Timestamp(Date(now - 1_000L))
            ),
            EngagementEvent(
                id = "e_old",
                userId = "u4",
                targetType = "tool",
                targetId = "tool_figma",
                eventType = "save",
                validEvent = true,
                createdAt = Timestamp(Date(now - 5 * 86_400_000L)) // outside 1-day window
            )
        )

        val status = TrendingEngine.scoreEventsForTarget(
            events = events,
            targetType = "tool",
            targetId = "tool_figma",
            windowDays = TrendingEngine.windowDaysForSection(TrendingSection.TODAY),
            nowMillis = now
        )
        assertTrue(status is RankingStatus.Ranked)
        val ranked = status as RankingStatus.Ranked
        assertEquals(2, ranked.eventCount)
        assertEquals(8.0, ranked.score, 0.001)
    }

    @Test
    fun `deterministicEventId prevents duplicate same-day same-user events`() {
        val id1 = TrendingEngine.deterministicEventId("u1", "tool", "tool_canva", "save", "2026-11-20")
        val id2 = TrendingEngine.deterministicEventId("u1", "tool", "tool_canva", "save", "2026-11-20")
        val idNextDay = TrendingEngine.deterministicEventId("u1", "tool", "tool_canva", "save", "2026-11-21")
        assertEquals(id1, id2)
        assertTrue(id1 != idNextDay)
    }
}
