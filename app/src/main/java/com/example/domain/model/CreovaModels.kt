package com.example.domain.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class ToolCategory(val id: String, val labelAr: String, val labelEn: String) {
    VIDEO_EDITING("video_editing", "مونتاج الفيديو", "Video Editing"),
    GRAPHIC_DESIGN("graphic_design", "التصميم الجرافيكي", "Graphic Design"),
    PHOTOS_VIDEOS("photos_videos", "صور وفيديوهات", "Photos & Videos"),
    FONTS("fonts", "الخطوط", "Fonts"),
    AUDIO("audio", "الصوتيات", "Audio"),
    EFFECTS("effects", "المؤثرات", "Effects");

    companion object {
        fun fromId(id: String?): ToolCategory? = entries.firstOrNull { it.id == id }
    }
}

enum class PricingTier(val wireValue: String) {
    FREE("free"),
    FREEMIUM("freemium"),
    PAID("paid"),
    OPEN_SOURCE("open_source");

    companion object {
        fun fromWire(value: String?): PricingTier =
            entries.firstOrNull { it.wireValue == value } ?: FREEMIUM
    }
}

enum class ResourceCategory(val id: String, val labelAr: String, val labelEn: String) {
    FONTS("fonts", "خطوط", "Fonts"),
    VIDEO_TEMPLATES("video_templates", "قوالب فيديو", "Video Templates"),
    PHOTO_TEMPLATES("photo_templates", "قوالب صور", "Photo Templates"),
    REELS_TEMPLATES("reels_templates", "قوالب ريلز", "Reels Templates"),
    YOUTUBE_TEMPLATES("youtube_templates", "قوالب يوتيوب", "YouTube Templates"),
    THUMBNAILS("thumbnails", "صور مصغرة", "Thumbnails"),
    PRESETS("presets", "بريستات", "Presets"),
    LUTS("luts", "فلاتر ألوان LUTs", "LUTs"),
    TRANSITIONS("transitions", "انتقالات", "Transitions"),
    SOUND_EFFECTS("sound_effects", "مؤثرات صوتية", "Sound Effects"),
    MUSIC("music", "موسيقى", "Music"),
    STOCK_VIDEOS("stock_videos", "فيديوهات ستوك", "Stock Videos"),
    STOCK_PHOTOS("stock_photos", "صور ستوك", "Stock Photos"),
    PNG_ASSETS("png_assets", "ملفات PNG شفافة", "PNG Assets"),
    BACKGROUNDS("backgrounds", "خلفيات", "Backgrounds"),
    TEXTURES("textures", "خامات وتكستشر", "Textures"),
    ICONS("icons", "أيقونات", "Icons"),
    MOCKUPS("mockups", "موك أب", "Mockups"),
    DESIGN_FILES("design_files", "ملفات تصميم مفتوحة", "Design Files");

    companion object {
        fun fromId(id: String?): ResourceCategory =
            entries.firstOrNull { it.id == id } ?: DESIGN_FILES
    }
}

enum class PostType(val wireValue: String, val labelAr: String, val labelEn: String) {
    IMAGE("image", "صورة", "Image"),
    VIDEO("video", "فيديو", "Video"),
    RESOURCE("resource", "مورد إبداعي", "Resource"),
    FONT_DISCOVERY("font_discovery", "اكتشاف خط", "Font Discovery"),
    DESIGN_SHOWCASE("design_showcase", "عرض تصميم", "Design Showcase"),
    EDITING_TUTORIAL("editing_tutorial", "شرح مونتاج", "Editing Tutorial"),
    CREATIVE_DISCOVERY("creative_discovery", "اكتشاف أداة", "Creative Discovery");

    companion object {
        fun fromWire(value: String?): PostType =
            entries.firstOrNull { it.wireValue == value } ?: CREATIVE_DISCOVERY
    }
}

enum class TrendingSection {
    TODAY,
    THIS_WEEK,
    THIS_MONTH,
    MOST_SAVED,
    HIGHEST_RATED,
    RECENTLY_ADDED
}

enum class CommunityFeedTab {
    FOR_YOU,
    FOLLOWING,
    TRENDING,
    LATEST
}

enum class ResourceSort {
    POPULAR,
    NEWEST,
    MOST_SAVED
}

@Keep
@IgnoreExtraProperties
data class UserPrivate(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val preferredLanguage: String = "ar",
    val themeMode: String = "system",
    val aiModel: String = "gemini-3.8-flash",
    val accountStatus: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val bio: String = "",
    val avatarUrl: String = "",
    val specialty: String = "",
    val publicProfile: Boolean = true,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postsCount: Long = 0L,
    val resourcesCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CreativeTool(
    @DocumentId val id: String = "",
    val name: String = "",
    val nameAr: String = "",
    val description: String = "",
    val descriptionAr: String = "",
    val categories: List<String> = emptyList(),
    val officialUrl: String = "",
    val pricing: String = "freemium",
    val platforms: List<String> = emptyList(),
    val ratingAvg: Double = 0.0,
    val ratingCount: Long = 0L,
    val savesCount: Long = 0L,
    val verified: Boolean = true,
    val lastVerifiedDate: String = "2026-11-15",
    val accentHex: String = "#8B5CF6",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val pricingTier: PricingTier get() = PricingTier.fromWire(pricing)
}

@Keep
@IgnoreExtraProperties
data class CreativeResource(
    @DocumentId val id: String = "",
    val creatorId: String = "",
    val creatorName: String = "",
    val title: String = "",
    val titleAr: String = "",
    val description: String = "",
    val descriptionAr: String = "",
    val category: String = ResourceCategory.DESIGN_FILES.id,
    val tags: List<String> = emptyList(),
    val previewImageUrl: String = "",
    val fileFormat: String = "",
    val fileSizeLabel: String = "",
    val licenseInfo: String = "",
    val downloadUrl: String = "",
    val savesCount: Long = 0L,
    val likesCount: Long = 0L,
    val moderationStatus: String = "approved",
    val seeded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CommunityPost(
    @DocumentId val id: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorUsername: String = "",
    val authorAvatarUrl: String = "",
    val postType: String = PostType.CREATIVE_DISCOVERY.wireValue,
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val tags: List<String> = emptyList(),
    val mediaDataUrl: String = "",
    val mediaUrl: String = "",
    val linkedUrl: String = "",
    val likesCount: Long = 0L,
    val commentsCount: Long = 0L,
    val savesCount: Long = 0L,
    val sharesCount: Long = 0L,
    val moderationStatus: String = "active",
    val seeded: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val resolvedType: PostType get() = PostType.fromWire(postType)
}

@Keep
@IgnoreExtraProperties
data class PostComment(
    @DocumentId val id: String = "",
    val postId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val authorAvatarUrl: String = "",
    val parentCommentId: String = "",
    val body: String = "",
    val moderationStatus: String = "active",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SavedItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val title: String = "",
    val subtitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class NotificationItem(
    @DocumentId val id: String = "",
    val recipientId: String = "",
    val actorId: String = "",
    val actorName: String = "",
    val type: String = "like",
    val targetId: String = "",
    val message: String = "",
    val seen: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class ModerationReport(
    @DocumentId val id: String = "",
    val reporterId: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val reason: String = "",
    val details: String = "",
    val status: String = "open",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class EngagementEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val targetType: String = "",
    val targetId: String = "",
    val eventType: String = "",
    val dayKey: String = "",
    val validEvent: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null
)
