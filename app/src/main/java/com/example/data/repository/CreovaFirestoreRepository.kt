package com.example.data.repository

import com.example.core.security.OperationType
import com.example.core.security.SafeUrlLauncher
import com.example.core.security.handleFirestoreError
import com.example.domain.analytics.TrendingEngine
import com.example.domain.model.CommunityPost
import com.example.domain.model.CreativeResource
import com.example.domain.model.CreativeTool
import com.example.domain.model.EngagementEvent
import com.example.domain.model.ModerationReport
import com.example.domain.model.NotificationItem
import com.example.domain.model.PostComment
import com.example.domain.model.PublicProfile
import com.example.domain.model.SavedItem
import com.example.domain.model.UserPrivate
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import android.net.Uri
import android.util.Base64
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class CreovaFirestoreRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before performing this action.")

    private fun <T> Flow<T>.logFirestoreErrors(op: OperationType, path: String, expectedUid: String? = null): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && (expectedUid == null || auth.currentUser?.uid == expectedUid)) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }

    // --- PUBLIC / CATALOG STREAMS (Aligned 1:1 with firestore.rules) ---

    fun observeVerifiedTools(): Flow<List<CreativeTool>> =
        db.collection("tools")
            .whereEqualTo("verified", true)
            .snapshots()
            .map { snap -> snap.toObjects(CreativeTool::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "tools")

    fun observeApprovedResources(): Flow<List<CreativeResource>> =
        db.collection("resources")
            .whereEqualTo("moderationStatus", "approved")
            .snapshots()
            .map { snap -> snap.toObjects(CreativeResource::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "resources")

    fun observeActivePosts(): Flow<List<CommunityPost>> =
        db.collection("posts")
            .whereEqualTo("moderationStatus", "active")
            .snapshots()
            .map { snap -> snap.toObjects(CommunityPost::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "posts")

    fun observePublicProfiles(): Flow<List<PublicProfile>> =
        db.collection("public_profiles")
            .whereEqualTo("publicProfile", true)
            .snapshots()
            .map { snap -> snap.toObjects(PublicProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "public_profiles")

    fun observeValidEngagementEvents(): Flow<List<EngagementEvent>> =
        db.collection("engagement_events")
            .whereEqualTo("validEvent", true)
            .snapshots()
            .map { snap -> snap.toObjects(EngagementEvent::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "engagement_events")

    fun observePostComments(postId: String): Flow<List<PostComment>> =
        db.collection("posts").document(postId)
            .collection("comments")
            .whereEqualTo("moderationStatus", "active")
            .snapshots()
            .map { snap -> snap.toObjects(PostComment::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "posts/$postId/comments")

    // --- AUTHENTICATED USER STREAMS ---

    fun observeUserPrivate(uid: String): Flow<UserPrivate?> =
        db.collection("users").document(uid)
            .snapshots()
            .map { snap -> snap.toObject(UserPrivate::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.GET, "users/$uid", uid)

    fun observeSavedItems(uid: String): Flow<List<SavedItem>> =
        db.collection("saved_items")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap -> snap.toObjects(SavedItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "saved_items", uid)

    fun observeLikedTargetKeys(uid: String): Flow<Set<String>> =
        db.collection("likes")
            .whereEqualTo("userId", uid)
            .snapshots()
            .map { snap ->
                snap.documents.mapNotNull { doc ->
                    val type = doc.getString("targetType")
                    val id = doc.getString("targetId")
                    if (!type.isNullOrBlank() && !id.isNullOrBlank()) "${type}_$id" else null
                }.toSet()
            }
            .logFirestoreErrors(OperationType.LIST, "likes", uid)

    fun observeFollowingIds(uid: String): Flow<Set<String>> =
        db.collection("follows")
            .whereEqualTo("followerId", uid)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.getString("followingId") }.toSet() }
            .logFirestoreErrors(OperationType.LIST, "follows", uid)

    fun observeBlockedUserIds(uid: String): Flow<Set<String>> =
        db.collection("blocks")
            .whereEqualTo("blockerId", uid)
            .snapshots()
            .map { snap -> snap.documents.mapNotNull { it.getString("blockedUserId") }.toSet() }
            .logFirestoreErrors(OperationType.LIST, "blocks", uid)

    fun observeNotifications(uid: String): Flow<List<NotificationItem>> =
        db.collection("notifications")
            .whereEqualTo("recipientId", uid)
            .snapshots()
            .map { snap -> snap.toObjects(NotificationItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "notifications", uid)

    suspend fun checkIsAdmin(uid: String): Boolean = try {
        db.collection("admins").document(uid).get().await().exists()
    } catch (_: Exception) {
        false
    }

    fun observeOpenReports(): Flow<List<ModerationReport>> =
        db.collection("reports")
            .whereEqualTo("status", "open")
            .snapshots()
            .map { snap -> snap.toObjects(ModerationReport::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "reports")

    fun observePendingResources(): Flow<List<CreativeResource>> =
        db.collection("resources")
            .whereEqualTo("moderationStatus", "pending")
            .snapshots()
            .map { snap -> snap.toObjects(CreativeResource::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
            .logFirestoreErrors(OperationType.LIST, "resources")

    // --- MUTATIONS (Always Result<T>, FieldValue.serverTimestamp(), Rule-Aligned) ---

    suspend fun updateUserPreferences(
        uid: String,
        language: String? = null,
        themeMode: String? = null,
        aiModel: String? = null
    ): Result<Unit> = try {
        val currentUid = requireUserId()
        check(currentUid == uid) { "Cannot update another user's preferences." }
        val updates = buildMap<String, Any> {
            language?.let { put("preferredLanguage", it) }
            themeMode?.let { put("themeMode", it) }
            aiModel?.let { put("aiModel", it) }
            put("updatedAt", FieldValue.serverTimestamp())
        }
        db.collection("users").document(uid).update(updates).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "users/$uid")
        Result.failure(e)
    }

    suspend fun updatePublicProfile(
        uid: String,
        displayName: String,
        username: String,
        bio: String,
        specialty: String
    ): Result<Unit> = try {
        val currentUid = requireUserId()
        check(currentUid == uid) { "Cannot edit another user's profile." }
        val cleanName = displayName.trim().take(60)
        val cleanUsername = username.trim().lowercase().replace(Regex("[^a-z0-9_]"), "_").take(32)
        require(cleanName.isNotEmpty()) { "Display name is required." }
        require(cleanUsername.length >= 2) { "Username must be at least 2 characters." }
        db.collection("public_profiles").document(uid).update(
            mapOf(
                "displayName" to cleanName,
                "username" to cleanUsername,
                "bio" to bio.trim().take(300),
                "specialty" to specialty.trim().take(60),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "public_profiles/$uid")
        Result.failure(e)
    }

    suspend fun toggleSave(
        targetType: String,
        targetId: String,
        title: String,
        subtitle: String,
        currentlySaved: Boolean
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to save items."))
        val docId = "${uid}_${targetType}_${targetId}"
        val path = "saved_items/$docId"
        return try {
            val ref = db.collection("saved_items").document(docId)
            if (currentlySaved) {
                ref.delete().await()
                Result.success(false)
            } else {
                val payload = mapOf(
                    "userId" to uid,
                    "targetType" to targetType,
                    "targetId" to targetId,
                    "title" to title.trim().take(140).ifBlank { targetId },
                    "subtitle" to subtitle.trim().take(140),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
                recordEngagementEvent(targetType, targetId, "save")
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun toggleLike(
        targetType: String,
        targetId: String,
        targetOwnerId: String?,
        targetTitle: String,
        currentlyLiked: Boolean
    ): Result<Boolean> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to like items."))
        val docId = "${uid}_${targetType}_${targetId}"
        val path = "likes/$docId"
        return try {
            val likeRef = db.collection("likes").document(docId)
            if (currentlyLiked) {
                likeRef.delete().await()
                adjustLikeCounterIfRemoteExists(targetType, targetId, -1)
                Result.success(false)
            } else {
                likeRef.set(
                    mapOf(
                        "userId" to uid,
                        "targetType" to targetType,
                        "targetId" to targetId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                adjustLikeCounterIfRemoteExists(targetType, targetId, 1)
                recordEngagementEvent(targetType, targetId, "like")
                if (!targetOwnerId.isNullOrBlank() && targetOwnerId != uid && !targetOwnerId.startsWith("creator_") && targetOwnerId != "creova_editorial") {
                    sendNotification(
                        recipientId = targetOwnerId,
                        type = "like",
                        targetId = targetId,
                        message = "أعجب بمشاركتك: ${targetTitle.take(60)}"
                    )
                }
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    private suspend fun adjustLikeCounterIfRemoteExists(targetType: String, targetId: String, delta: Long) {
        val collection = if (targetType == "post") "posts" else if (targetType == "resource") "resources" else return
        try {
            val ref = db.collection(collection).document(targetId)
            val snap = ref.get().await()
            if (snap.exists()) {
                val current = snap.getLong("likesCount") ?: 0L
                val next = (current + delta).coerceAtLeast(0L)
                if (next != current) {
                    ref.update(mapOf("likesCount" to next, "updatedAt" to FieldValue.serverTimestamp())).await()
                }
            }
        } catch (_: Exception) {
            // Seeded local items may not have remote Firestore doc yet
        }
    }

    private suspend fun resolvePostMediaUrl(uid: String, imageValue: String?): String? {
        if (imageValue.isNullOrBlank()) return null
        if (!imageValue.startsWith("data:image/")) {
            require(SafeUrlLauncher.isSafeHttpsUrl(imageValue)) { "Post image URL must use HTTPS." }
            return imageValue.take(500)
        }
        val encoded = imageValue.substringAfter(",", "")
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        require(bytes.isNotEmpty() && bytes.size <= 210_000) { "Post image is empty or exceeds the allowed size." }
        val objectRef = storage.reference.child("post_media/$uid/${java.util.UUID.randomUUID()}.jpg")
        val metadata = StorageMetadata.Builder().setContentType("image/jpeg").build()
        objectRef.putBytes(bytes, metadata).await()
        return objectRef.downloadUrl.await().toString()
    }

    suspend fun createPost(
        authorProfile: PublicProfile?,
        postType: String,
        category: String,
        title: String,
        description: String,
        tags: List<String>,
        mediaDataUrl: String?,
        linkedUrl: String?
    ): Result<String> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Google Sign-In required to publish."))
        val uid = user.uid
        val cleanTitle = title.trim().take(140)
        val cleanDesc = description.trim().take(2000)
        if (cleanTitle.length < 2 || cleanDesc.length < 2) {
            return Result.failure(IllegalArgumentException("Title and description must be at least 2 characters."))
        }
        val cleanLinked = linkedUrl?.trim()?.takeIf { it.isNotBlank() }
        if (cleanLinked != null && !SafeUrlLauncher.isSafeHttpsUrl(cleanLinked)) {
            return Result.failure(IllegalArgumentException("Linked URL must be a safe https:// link."))
        }
        val docRef = db.collection("posts").document()
        return try {
            val uploadedMediaUrl = resolvePostMediaUrl(uid, mediaDataUrl)
            val payload = buildMap<String, Any> {
                put("authorId", uid)
                put("authorName", (authorProfile?.displayName ?: user.displayName ?: "Creator").take(80))
                put("authorUsername", (authorProfile?.username ?: "creator_${uid.take(4)}").take(40))
                val avatar = authorProfile?.avatarUrl?.takeIf { it.isNotBlank() } ?: user.photoUrl?.toString()
                if (!avatar.isNullOrBlank() && avatar.length <= 500) put("authorAvatarUrl", avatar)
                put("postType", postType)
                put("category", category.take(60))
                put("title", cleanTitle)
                put("description", cleanDesc)
                put("tags", tags.map { it.trim().take(24) }.filter { it.isNotBlank() }.take(10))
                if (!uploadedMediaUrl.isNullOrBlank()) put("mediaUrl", uploadedMediaUrl)
                if (cleanLinked != null) put("linkedUrl", cleanLinked)
                put("likesCount", 0L)
                put("commentsCount", 0L)
                put("savesCount", 0L)
                put("sharesCount", 0L)
                put("moderationStatus", "active")
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updatePost(
        postId: String,
        category: String,
        title: String,
        description: String,
        tags: List<String>,
        mediaDataUrl: String?,
        linkedUrl: String?
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to edit a post."))
        val cleanTitle = title.trim().take(140)
        val cleanDescription = description.trim().take(2000)
        val cleanLink = linkedUrl?.trim()?.takeIf { it.isNotBlank() }
        if (cleanTitle.length < 2 || cleanDescription.length < 2) {
            return Result.failure(IllegalArgumentException("Title and description must be at least 2 characters."))
        }
        if (cleanLink != null && !SafeUrlLauncher.isSafeHttpsUrl(cleanLink)) {
            return Result.failure(IllegalArgumentException("Linked URL must be a safe https:// link."))
        }
        return try {
            val ref = db.collection("posts").document(postId)
            val snapshot = ref.get().await()
            if (!snapshot.exists() || snapshot.getString("authorId") != uid) {
                return Result.failure(IllegalAccessException("You can only edit your own posts."))
            }
            val uploadedMediaUrl = resolvePostMediaUrl(uid, mediaDataUrl)
            val updates = mutableMapOf<String, Any>(
                "category" to category.take(60),
                "title" to cleanTitle,
                "description" to cleanDescription,
                "tags" to tags.map { it.trim().take(24) }.filter { it.isNotBlank() }.take(10),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            updates["mediaDataUrl"] = FieldValue.delete()
            if (uploadedMediaUrl.isNullOrBlank()) updates["mediaUrl"] = FieldValue.delete()
            else updates["mediaUrl"] = uploadedMediaUrl
            if (cleanLink == null) updates["linkedUrl"] = FieldValue.delete() else updates["linkedUrl"] = cleanLink
            ref.update(updates).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to delete a post."))
        return try {
            val ref = db.collection("posts").document(postId)
            val snapshot = ref.get().await()
            if (!snapshot.exists() || snapshot.getString("authorId") != uid) {
                return Result.failure(IllegalAccessException("You can only delete your own posts."))
            }
            ref.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "posts/$postId")
            Result.failure(e)
        }
    }

    suspend fun addComment(
        postId: String,
        postAuthorId: String?,
        authorProfile: PublicProfile?,
        body: String,
        parentCommentId: String? = null
    ): Result<Unit> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Google Sign-In required to comment."))
        val uid = user.uid
        val cleanBody = body.trim().take(1000)
        if (cleanBody.isEmpty()) return Result.failure(IllegalArgumentException("Comment cannot be empty."))
        val commentRef = db.collection("posts").document(postId).collection("comments").document()
        return try {
            val postSnap = db.collection("posts").document(postId).get().await()
            if (!postSnap.exists()) {
                return Result.failure(IllegalStateException("Comments are available on live community posts."))
            }
            val payload = buildMap<String, Any> {
                put("postId", postId)
                put("authorId", uid)
                put("authorName", (authorProfile?.displayName ?: user.displayName ?: "Creator").take(80))
                val avatar = authorProfile?.avatarUrl?.takeIf { it.isNotBlank() } ?: user.photoUrl?.toString()
                if (!avatar.isNullOrBlank() && avatar.length <= 500) put("authorAvatarUrl", avatar)
                if (!parentCommentId.isNullOrBlank()) put("parentCommentId", parentCommentId.take(128))
                put("body", cleanBody)
                put("moderationStatus", "active")
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            commentRef.set(payload).await()
            val currentCount = postSnap.getLong("commentsCount") ?: 0L
            db.collection("posts").document(postId).update(
                mapOf("commentsCount" to currentCount + 1L, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            recordEngagementEvent("post", postId, "comment")
            if (!postAuthorId.isNullOrBlank() && postAuthorId != uid) {
                sendNotification(
                    recipientId = postAuthorId,
                    type = if (parentCommentId.isNullOrBlank()) "comment" else "reply",
                    targetId = postId,
                    message = cleanBody.take(120)
                )
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, commentRef.path)
            Result.failure(e)
        }
    }

    suspend fun uploadResourceFile(uri: Uri, mimeType: String, onProgress: (Float) -> Unit): Result<String> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to upload a resource."))
        val allowedTypes = setOf("application/pdf", "image/jpeg", "image/png", "image/webp", "font/ttf", "font/otf", "font/woff", "font/woff2")
        if (mimeType !in allowedTypes) return Result.failure(IllegalArgumentException("Unsupported resource file type."))
        return try {
            val objectRef = storage.reference.child("resource_uploads/$uid/${java.util.UUID.randomUUID()}")
            val metadata = StorageMetadata.Builder().setContentType(mimeType).build()
            val task = objectRef.putFile(uri, metadata)
            task.addOnProgressListener { snapshot ->
                if (snapshot.totalByteCount > 0L) onProgress((snapshot.bytesTransferred.toFloat() / snapshot.totalByteCount).coerceIn(0f, 1f))
            }
            task.await()
            val url = objectRef.downloadUrl.await().toString()
            onProgress(1f)
            Result.success(url)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun submitResource(
        creatorProfile: PublicProfile?,
        title: String,
        titleAr: String,
        description: String,
        descriptionAr: String,
        category: String,
        tags: List<String>,
        fileFormat: String,
        fileSizeLabel: String,
        licenseInfo: String,
        downloadUrl: String
    ): Result<String> {
        val user = auth.currentUser
            ?: return Result.failure(IllegalStateException("Google Sign-In required to submit a resource."))
        if (!SafeUrlLauncher.isSafeHttpsUrl(downloadUrl)) {
            return Result.failure(IllegalArgumentException("Resource URL must be a valid non-executable https:// link."))
        }
        val cleanTitle = title.trim().take(120)
        val cleanTitleAr = titleAr.trim().ifBlank { cleanTitle }.take(120)
        val cleanDesc = description.trim().take(1000)
        val cleanDescAr = descriptionAr.trim().ifBlank { cleanDesc }.take(1000)
        if (cleanTitle.length < 2 || cleanDesc.length < 4) {
            return Result.failure(IllegalArgumentException("Please enter a valid title and description."))
        }
        val docRef = db.collection("resources").document()
        return try {
            val payload = mapOf(
                "creatorId" to user.uid,
                "creatorName" to (creatorProfile?.displayName ?: user.displayName ?: "Creator").take(80),
                "title" to cleanTitle,
                "titleAr" to cleanTitleAr,
                "description" to cleanDesc,
                "descriptionAr" to cleanDescAr,
                "category" to category.take(60),
                "tags" to tags.map { it.trim().take(24) }.filter { it.isNotBlank() }.take(10),
                "fileFormat" to fileFormat.trim().uppercase().take(30).ifBlank { "LINK" },
                "fileSizeLabel" to fileSizeLabel.trim().take(30).ifBlank { "External" },
                "licenseInfo" to licenseInfo.trim().take(120).ifBlank { "Creator Verified License" },
                "downloadUrl" to downloadUrl.trim().take(500),
                "savesCount" to 0L,
                "likesCount" to 0L,
                "moderationStatus" to "approved",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun toggleFollow(targetUserId: String, currentlyFollowing: Boolean): Result<Boolean> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to follow creators."))
        if (uid == targetUserId) return Result.failure(IllegalArgumentException("Cannot follow yourself."))
        val docId = "${uid}_${targetUserId}"
        return try {
            val ref = db.collection("follows").document(docId)
            if (currentlyFollowing) {
                ref.delete().await()
                Result.success(false)
            } else {
                ref.set(
                    mapOf(
                        "followerId" to uid,
                        "followingId" to targetUserId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                if (!targetUserId.startsWith("creator_")) {
                    sendNotification(
                        recipientId = targetUserId,
                        type = "follow",
                        targetId = uid,
                        message = "بدأ بمتابعتك على CREOVA"
                    )
                }
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "follows/$docId")
            Result.failure(e)
        }
    }

    suspend fun submitReport(
        targetType: String,
        targetId: String,
        reason: String,
        details: String = ""
    ): Result<Unit> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required to submit a report."))
        val ref = db.collection("reports").document()
        return try {
            val payload = buildMap<String, Any> {
                put("reporterId", uid)
                put("targetType", targetType)
                put("targetId", targetId.take(128))
                put("reason", reason.trim().take(120).ifBlank { "Inappropriate content" })
                if (details.isNotBlank()) put("details", details.trim().take(500))
                put("status", "open")
                put("createdAt", FieldValue.serverTimestamp())
                put("updatedAt", FieldValue.serverTimestamp())
            }
            ref.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, ref.path)
            Result.failure(e)
        }
    }

    suspend fun toggleBlockUser(targetUserId: String, currentlyBlocked: Boolean): Result<Boolean> {
        val uid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Google Sign-In required."))
        if (uid == targetUserId) return Result.failure(IllegalArgumentException("Cannot block yourself."))
        val docId = "${uid}_${targetUserId}"
        return try {
            val ref = db.collection("blocks").document(docId)
            if (currentlyBlocked) {
                ref.delete().await()
                Result.success(false)
            } else {
                ref.set(
                    mapOf(
                        "blockerId" to uid,
                        "blockedUserId" to targetUserId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
                Result.success(true)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, "blocks/$docId")
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> = try {
        requireUserId()
        db.collection("notifications").document(notificationId)
            .update(mapOf("seen" to true))
            .await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "notifications/$notificationId")
        Result.failure(e)
    }

    suspend fun recordEngagementEvent(
        targetType: String,
        targetId: String,
        eventType: String
    ) {
        val uid = auth.currentUser?.uid ?: return
        val dayKey = TrendingEngine.currentUtcDayKey()
        val eventId = TrendingEngine.deterministicEventId(uid, targetType, targetId, eventType, dayKey)
        try {
            val ref = db.collection("engagement_events").document(eventId)
            if (!ref.get().await().exists()) {
                ref.set(
                    mapOf(
                        "userId" to uid,
                        "targetType" to targetType,
                        "targetId" to targetId.take(128),
                        "eventType" to eventType,
                        "dayKey" to dayKey,
                        "validEvent" to true,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        } catch (_: Exception) {
            // Anti-spam deduplication silently ignores duplicate day events
        }
    }

    // --- ADMIN MODERATION ACTIONS (Enforced by server-side /admins/{uid} rules) ---

    suspend fun moderateResource(resourceId: String, newStatus: String): Result<Unit> = try {
        requireUserId()
        db.collection("resources").document(resourceId).update(
            mapOf("moderationStatus" to newStatus, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "resources/$resourceId")
        Result.failure(e)
    }

    suspend fun updateReportStatus(reportId: String, status: String): Result<Unit> = try {
        requireUserId()
        db.collection("reports").document(reportId).update(
            mapOf("status" to status, "updatedAt" to FieldValue.serverTimestamp())
        ).await()
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        handleFirestoreError(e, OperationType.UPDATE, "reports/$reportId")
        Result.failure(e)
    }

    private suspend fun sendNotification(
        recipientId: String,
        type: String,
        targetId: String,
        message: String
    ) {
        val actor = auth.currentUser ?: return
        try {
            db.collection("notifications").document().set(
                mapOf(
                    "recipientId" to recipientId,
                    "actorId" to actor.uid,
                    "actorName" to (actor.displayName ?: "CREOVA User").take(80),
                    "type" to type,
                    "targetId" to targetId.take(128),
                    "message" to message.take(300),
                    "seen" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (_: Exception) {
        }
    }
}
