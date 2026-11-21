package com.example.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.AppThemeMode
import com.example.data.catalog.CuratedCatalog
import com.example.data.preferences.CreovaPreferences
import com.example.data.preferences.GeminiModelOption
import com.example.data.preferences.UserPrefsSnapshot
import com.example.data.repository.AuthRepository
import com.example.data.repository.CreovaFirestoreRepository
import com.example.data.repository.SignInOutcome
import com.example.domain.analytics.RankingStatus
import com.example.domain.analytics.TrendingEngine
import com.example.domain.model.CommunityPost
import com.example.domain.model.CreativeResource
import com.example.domain.model.CreativeTool
import com.example.domain.model.EngagementEvent
import com.example.domain.model.ModerationReport
import com.example.domain.model.NotificationItem
import com.example.domain.model.PostComment
import com.example.domain.model.PublicProfile
import com.example.domain.model.ResourceCategory
import com.example.domain.model.SavedItem
import com.example.domain.model.ToolCategory
import com.example.domain.model.TrendingSection
import com.example.domain.model.UserPrivate
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class GlobalSearchResults(
    val query: String = "",
    val tools: List<CreativeTool> = emptyList(),
    val resources: List<CreativeResource> = emptyList(),
    val posts: List<CommunityPost> = emptyList(),
    val creators: List<PublicProfile> = emptyList(),
    val toolCategories: List<ToolCategory> = emptyList(),
    val resourceCategories: List<ResourceCategory> = emptyList()
)

sealed interface CatalogSyncState {
    data object Ready : CatalogSyncState
    data class Error(val message: String) : CatalogSyncState
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CreovaAppViewModel(
    private val authRepository: AuthRepository,
    private val firestoreRepository: CreovaFirestoreRepository,
    private val preferences: CreovaPreferences
) : ViewModel() {

    private companion object {
        const val TAG = "CreovaAppVM"
        const val STOP_MILLIS = 5000L
    }

    private fun <T> Flow<T>.safeStream(fallback: T, label: String): Flow<T> =
        catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val root = firestoreError ?: error
            Log.w(TAG, "Stream error ($label): ${root.message}", root)
            _syncError.value = CatalogSyncState.Error(root.message ?: "Sync error")
            emit(fallback)
        }

    private val _syncError = MutableStateFlow<CatalogSyncState>(CatalogSyncState.Ready)
    val syncError: StateFlow<CatalogSyncState> = _syncError.asStateFlow()

    val prefs: StateFlow<UserPrefsSnapshot> = preferences.prefsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), UserPrefsSnapshot())

    val currentUser: StateFlow<FirebaseUser?> = authRepository.authStateFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), authRepository.currentUser)

    private val _showSignInSheet = MutableStateFlow(false)
    val showSignInSheet: StateFlow<Boolean> = _showSignInSheet.asStateFlow()

    private val _isSigningIn = MutableStateFlow(false)
    val isSigningIn: StateFlow<Boolean> = _isSigningIn.asStateFlow()

    private val _authMessage = MutableStateFlow<String?>(null)
    val authMessage: StateFlow<String?> = _authMessage.asStateFlow()

    private val _snackbarEvents = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val snackbarEvents = _snackbarEvents.asSharedFlow()

    val isAdmin: StateFlow<Boolean> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(false)
        else flowOf(firestoreRepository.checkIsAdmin(user.uid))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), false)

    // --- PUBLIC / CATALOG MERGED WITH SEEDED DATA ---

    val tools: StateFlow<List<CreativeTool>> = firestoreRepository.observeVerifiedTools()
        .safeStream(emptyList(), "tools")
        .map { remote ->
            val remoteMap = remote.associateBy { it.id }
            val merged = CuratedCatalog.verifiedTools.map { seed -> remoteMap[seed.id] ?: seed }
            val extras = remote.filter { r -> CuratedCatalog.verifiedTools.none { it.id == r.id } }
            merged + extras
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), CuratedCatalog.verifiedTools)

    val resources: StateFlow<List<CreativeResource>> = firestoreRepository.observeApprovedResources()
        .safeStream(emptyList(), "resources")
        .map { remote ->
            val remoteIds = remote.map { it.id }.toSet()
            val seeds = CuratedCatalog.curatedResources.filter { it.id !in remoteIds }
            remote.sortedByDescending { it.createdAt?.seconds ?: 0L } + seeds
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), CuratedCatalog.curatedResources)

    val blockedUserIds: StateFlow<Set<String>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptySet())
        else firestoreRepository.observeBlockedUserIds(user.uid).safeStream(emptySet(), "blocks")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptySet())

    val posts: StateFlow<List<CommunityPost>> = combine(
        firestoreRepository.observeActivePosts().safeStream(emptyList(), "posts"),
        blockedUserIds
    ) { remote, blocked ->
        val remoteIds = remote.map { it.id }.toSet()
        val seeds = CuratedCatalog.editorialPosts.filter { it.id !in remoteIds }
        (remote.sortedByDescending { it.createdAt?.seconds ?: 0L } + seeds)
            .filter { it.authorId !in blocked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), CuratedCatalog.editorialPosts)

    val creators: StateFlow<List<PublicProfile>> = combine(
        firestoreRepository.observePublicProfiles().safeStream(emptyList(), "public_profiles"),
        blockedUserIds
    ) { remote, blocked ->
        val remoteIds = remote.map { it.userId }.toSet()
        val seeds = CuratedCatalog.editorialCreators.filter { it.userId !in remoteIds }
        (remote + seeds).filter { it.userId !in blocked }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), CuratedCatalog.editorialCreators)

    val engagementEvents: StateFlow<List<EngagementEvent>> = firestoreRepository.observeValidEngagementEvents()
        .safeStream(emptyList(), "engagement_events")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyList())

    // --- AUTHENTICATED USER STREAMS ---

    val userPrivate: StateFlow<UserPrivate?> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(null)
        else firestoreRepository.observeUserPrivate(user.uid).safeStream(null, "users")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), null)

    val savedItems: StateFlow<List<SavedItem>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else firestoreRepository.observeSavedItems(user.uid).safeStream(emptyList(), "saved_items")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyList())

    val savedTargetKeys: StateFlow<Set<String>> = savedItems
        .map { items -> items.map { "${it.targetType}_${it.targetId}" }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptySet())

    val likedTargetKeys: StateFlow<Set<String>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptySet())
        else firestoreRepository.observeLikedTargetKeys(user.uid).safeStream(emptySet(), "likes")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptySet())

    val followingIds: StateFlow<Set<String>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptySet())
        else firestoreRepository.observeFollowingIds(user.uid).safeStream(emptySet(), "follows")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptySet())

    val notifications: StateFlow<List<NotificationItem>> = currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else firestoreRepository.observeNotifications(user.uid)
            .map { list -> list.sortedByDescending { it.createdAt?.seconds ?: 0L } }
            .safeStream(emptyList(), "notifications")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyList())

    // --- ADMIN MODERATION STREAMS ---
    val openReports: StateFlow<List<ModerationReport>> = isAdmin.flatMapLatest { admin ->
        if (!admin) flowOf(emptyList())
        else firestoreRepository.observeOpenReports().safeStream(emptyList(), "reports")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyList())

    val pendingResources: StateFlow<List<CreativeResource>> = isAdmin.flatMapLatest { admin ->
        if (!admin) flowOf(emptyList())
        else firestoreRepository.observePendingResources().safeStream(emptyList(), "pending_resources")
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), emptyList())

    // --- GLOBAL SEARCH (300ms Debounced) ---
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val searchResults: StateFlow<GlobalSearchResults> = combine(
        _searchQuery.debounce(300L),
        tools,
        resources,
        posts,
        creators
    ) { rawQ, allTools, allResources, allPosts, allCreators ->
        val q = rawQ.trim()
        if (q.isBlank()) {
            GlobalSearchResults()
        } else {
            val lq = q.lowercase()
            GlobalSearchResults(
                query = q,
                tools = allTools.filter {
                    it.name.lowercase().contains(lq) ||
                        it.nameAr.contains(q) ||
                        it.description.lowercase().contains(lq) ||
                        it.descriptionAr.contains(q) ||
                        it.categories.any { c -> c.contains(lq) }
                },
                resources = allResources.filter {
                    it.title.lowercase().contains(lq) ||
                        it.titleAr.contains(q) ||
                        it.description.lowercase().contains(lq) ||
                        it.descriptionAr.contains(q) ||
                        it.tags.any { t -> t.lowercase().contains(lq) } ||
                        it.category.contains(lq)
                },
                posts = allPosts.filter {
                    it.title.lowercase().contains(lq) ||
                        it.description.lowercase().contains(lq) ||
                        it.tags.any { t -> t.lowercase().contains(lq) }
                },
                creators = allCreators.filter {
                    it.displayName.lowercase().contains(lq) ||
                        it.username.lowercase().contains(lq) ||
                        it.specialty.lowercase().contains(lq)
                },
                toolCategories = ToolCategory.entries.filter {
                    it.labelEn.lowercase().contains(lq) || it.labelAr.contains(q)
                },
                resourceCategories = ResourceCategory.entries.filter {
                    it.labelEn.lowercase().contains(lq) || it.labelAr.contains(q)
                }
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_MILLIS), GlobalSearchResults())

    fun observeCommentsForPost(postId: String): Flow<List<PostComment>> =
        firestoreRepository.observePostComments(postId)
            .map { list -> list.sortedBy { it.createdAt?.seconds ?: Long.MAX_VALUE } }
            .safeStream(emptyList(), "comments")

    fun rankingForTool(toolId: String, section: TrendingSection, events: List<EngagementEvent>): RankingStatus =
        TrendingEngine.scoreEventsForTarget(
            events = events,
            targetType = "tool",
            targetId = toolId,
            windowDays = TrendingEngine.windowDaysForSection(section)
        )

    fun rankingForResource(resourceId: String, section: TrendingSection, events: List<EngagementEvent>): RankingStatus =
        TrendingEngine.scoreEventsForTarget(
            events = events,
            targetType = "resource",
            targetId = resourceId,
            windowDays = TrendingEngine.windowDaysForSection(section)
        )

    // --- AUTH & GUEST GATE ---

    fun attemptSilentSignIn(context: Context) {
        viewModelScope.launch {
            val cm = CredentialManager.create(context)
            val signedIn = authRepository.attemptAutoSignIn(context, cm)
            if (signedIn) {
                authRepository.currentUser?.let {
                    authRepository.ensureUserProfiles(it, prefs.value)
                }
            }
        }
    }

    fun requireAuthOrRun(action: () -> Unit) {
        if (currentUser.value != null) {
            action()
        } else {
            _authMessage.value = null
            _showSignInSheet.value = true
        }
    }

    fun openSignInSheet() {
        _authMessage.value = null
        _showSignInSheet.value = true
    }

    fun dismissSignInSheet() {
        _showSignInSheet.value = false
        _authMessage.value = null
    }

    fun triggerGoogleSignIn(activityContext: Context, cancelledLabel: String) {
        viewModelScope.launch {
            _isSigningIn.value = true
            _authMessage.value = null
            val cm = CredentialManager.create(activityContext)
            when (val outcome = authRepository.signInInteractive(activityContext, cm, prefs.value)) {
                is SignInOutcome.Success -> {
                    _isSigningIn.value = false
                    _showSignInSheet.value = false
                    _authMessage.value = null
                }
                is SignInOutcome.Cancelled -> {
                    _isSigningIn.value = false
                    _authMessage.value = cancelledLabel
                }
                is SignInOutcome.Error -> {
                    _isSigningIn.value = false
                    _authMessage.value = outcome.message
                }
            }
        }
    }

    fun signOut(context: Context) {
        viewModelScope.launch {
            val cm = CredentialManager.create(context)
            authRepository.signOut(cm)
        }
    }

    fun deleteAccount(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch {
            val cm = CredentialManager.create(context)
            authRepository.deleteCurrentAccount(cm)
                .onSuccess {
                    _snackbarEvents.tryEmit("Account deleted.")
                    onComplete()
                }
                .onFailure { e ->
                    _snackbarEvents.tryEmit(e.localizedMessage ?: "Account deletion failed.")
                }
        }
    }

    // --- PREFERENCES ---

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch {
            preferences.setLanguage(language)
            currentUser.value?.uid?.let { uid ->
                firestoreRepository.updateUserPreferences(uid, language = language.code)
            }
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferences.setThemeMode(mode)
            currentUser.value?.uid?.let { uid ->
                firestoreRepository.updateUserPreferences(uid, themeMode = mode.value)
            }
        }
    }

    fun setAiModel(model: GeminiModelOption) {
        viewModelScope.launch {
            preferences.setAiModel(model)
            currentUser.value?.uid?.let { uid ->
                firestoreRepository.updateUserPreferences(uid, aiModel = model.modelId)
            }
        }
    }

    fun setReducedMotion(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setReducedMotion(enabled)
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun commitSearchQuery(query: String) {
        viewModelScope.launch {
            preferences.addRecentSearch(query)
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            preferences.clearRecentSearches()
        }
    }

    // --- INTERACTIONS ---

    fun toggleSave(targetType: String, targetId: String, title: String, subtitle: String) {
        requireAuthOrRun {
            val key = "${targetType}_$targetId"
            val currentlySaved = key in savedTargetKeys.value
            viewModelScope.launch {
                firestoreRepository.toggleSave(targetType, targetId, title, subtitle, currentlySaved)
                    .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update saved state.") }
            }
        }
    }

    fun toggleLike(targetType: String, targetId: String, ownerId: String?, title: String) {
        requireAuthOrRun {
            val key = "${targetType}_$targetId"
            val currentlyLiked = key in likedTargetKeys.value
            viewModelScope.launch {
                firestoreRepository.toggleLike(targetType, targetId, ownerId, title, currentlyLiked)
                    .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update like.") }
            }
        }
    }

    fun toggleFollow(targetUserId: String) {
        requireAuthOrRun {
            val currentlyFollowing = targetUserId in followingIds.value
            viewModelScope.launch {
                firestoreRepository.toggleFollow(targetUserId, currentlyFollowing)
                    .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update follow.") }
            }
        }
    }

    fun toggleBlockUser(targetUserId: String, confirmationMsg: String) {
        requireAuthOrRun {
            val blocked = targetUserId in blockedUserIds.value
            viewModelScope.launch {
                firestoreRepository.toggleBlockUser(targetUserId, blocked)
                    .onSuccess { nowBlocked -> if (nowBlocked) _snackbarEvents.tryEmit(confirmationMsg) }
                    .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Action failed.") }
            }
        }
    }

    fun submitReport(targetType: String, targetId: String, reason: String, confirmationMsg: String) {
        requireAuthOrRun {
            viewModelScope.launch {
                firestoreRepository.submitReport(targetType, targetId, reason)
                    .onSuccess { _snackbarEvents.tryEmit(confirmationMsg) }
                    .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Report failed.") }
            }
        }
    }

    fun recordToolOpen(toolId: String) {
        if (currentUser.value != null) {
            viewModelScope.launch {
                firestoreRepository.recordEngagementEvent("tool", toolId, "tool_open")
            }
        }
    }

    fun recordResourceView(resourceId: String) {
        if (currentUser.value != null) {
            viewModelScope.launch {
                firestoreRepository.recordEngagementEvent("resource", resourceId, "resource_view")
            }
        }
    }

    fun recordShare(targetType: String, targetId: String) {
        if (currentUser.value != null) {
            viewModelScope.launch {
                firestoreRepository.recordEngagementEvent(targetType, targetId, "share")
            }
        }
    }

    fun createPost(
        postType: String,
        category: String,
        title: String,
        description: String,
        tags: List<String>,
        mediaDataUrl: String?,
        linkedUrl: String?,
        onSuccess: () -> Unit
    ) {
        requireAuthOrRun {
            val uid = currentUser.value?.uid ?: return@requireAuthOrRun
            val myProfile = creators.value.firstOrNull { it.userId == uid }
            viewModelScope.launch {
                firestoreRepository.createPost(
                    authorProfile = myProfile,
                    postType = postType,
                    category = category,
                    title = title,
                    description = description,
                    tags = tags,
                    mediaDataUrl = mediaDataUrl,
                    linkedUrl = linkedUrl
                ).onSuccess {
                    onSuccess()
                }.onFailure {
                    _snackbarEvents.tryEmit(it.localizedMessage ?: "Could not publish post.")
                }
            }
        }
    }

    fun addComment(postId: String, postAuthorId: String?, body: String, parentCommentId: String?) {
        requireAuthOrRun {
            val uid = currentUser.value?.uid ?: return@requireAuthOrRun
            val myProfile = creators.value.firstOrNull { it.userId == uid }
            viewModelScope.launch {
                firestoreRepository.addComment(postId, postAuthorId, myProfile, body, parentCommentId)
                    .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Could not post comment.") }
            }
        }
    }

    fun submitResource(
        title: String,
        titleAr: String,
        description: String,
        descriptionAr: String,
        category: String,
        tags: List<String>,
        fileFormat: String,
        fileSizeLabel: String,
        licenseInfo: String,
        downloadUrl: String,
        fileUri: Uri?,
        mimeType: String?,
        onProgress: (Float) -> Unit,
        onFinished: (Boolean) -> Unit,
        onSuccess: () -> Unit
    ) {
        requireAuthOrRun {
            val uid = currentUser.value?.uid ?: return@requireAuthOrRun
            val myProfile = creators.value.firstOrNull { it.userId == uid }
            viewModelScope.launch {
                val finalDownloadUrl = if (fileUri != null && !mimeType.isNullOrBlank()) {
                    firestoreRepository.uploadResourceFile(fileUri, mimeType, onProgress).getOrElse {
                        _snackbarEvents.tryEmit(it.localizedMessage ?: "File upload failed.")
                        onFinished(false)
                        return@launch
                    }
                } else if (fileUri != null) {
                    _snackbarEvents.tryEmit("Unsupported file type.")
                    onFinished(false)
                    return@launch
                } else downloadUrl
                firestoreRepository.submitResource(
                    creatorProfile = myProfile,
                    title = title,
                    titleAr = titleAr,
                    description = description,
                    descriptionAr = descriptionAr,
                    category = category,
                    tags = tags,
                    fileFormat = fileFormat,
                    fileSizeLabel = fileSizeLabel,
                    licenseInfo = licenseInfo,
                    downloadUrl = finalDownloadUrl
                ).onSuccess {
                    onFinished(true)
                    onSuccess()
                }.onFailure {
                    _snackbarEvents.tryEmit(it.localizedMessage ?: "Could not submit resource.")
                    onFinished(false)
                }
            }
        }
    }

    fun updateMyProfile(
        displayName: String,
        username: String,
        bio: String,
        specialty: String,
        onSuccess: () -> Unit
    ) {
        val uid = currentUser.value?.uid ?: return
        viewModelScope.launch {
            firestoreRepository.updatePublicProfile(uid, displayName, username, bio, specialty)
                .onSuccess { onSuccess() }
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Update failed.") }
        }
    }

    fun markAllNotificationsRead() {
        val unread = notifications.value.filter { !it.seen }
        if (unread.isEmpty()) return
        viewModelScope.launch {
            unread.forEach { firestoreRepository.markNotificationRead(it.id) }
        }
    }

    fun moderateResource(resourceId: String, status: String) {
        viewModelScope.launch {
            firestoreRepository.moderateResource(resourceId, status)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Moderation failed.") }
        }
    }

    fun updateReportStatus(reportId: String, status: String) {
        viewModelScope.launch {
            firestoreRepository.updateReportStatus(reportId, status)
                .onFailure { _snackbarEvents.tryEmit(it.localizedMessage ?: "Failed to update report.") }
        }
    }
}
