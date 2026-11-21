package com.example.ui.navigation

import android.net.Uri
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.example.core.i18n.AppLanguage
import com.example.core.i18n.LocalCreovaStrings
import com.example.domain.model.ToolCategory
import com.example.ui.components.GoogleSignInSheet
import com.example.ui.components.LiquidGlassBackground
import com.example.ui.components.LiquidGlassBottomBar
import com.example.ui.components.LiquidGlassNavRail
import com.example.ui.components.TopTabRoute
import com.example.ui.screens.AdminModerationScreen
import com.example.ui.screens.AiAssistantScreen
import com.example.ui.screens.CommunityScreen
import com.example.ui.screens.CreatePostScreen
import com.example.ui.screens.CreatorProfileScreen
import com.example.ui.screens.ExploreToolsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PostDetailScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.ResourceDetailScreen
import com.example.ui.screens.SavedItemsScreen
import com.example.ui.screens.SearchScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SubmitResourceScreen
import com.example.ui.screens.ToolDetailScreen
import com.example.ui.viewmodel.AiAssistantViewModel
import com.example.ui.viewmodel.CreovaAppViewModel

object CreovaRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val LIBRARY = "library"
    const val COMMUNITY = "community"
    const val PROFILE = "profile"
    const val SEARCH = "search"
    const val NOTIFICATIONS = "notifications"
    const val CREATE_POST = "create_post"
    const val SUBMIT_RESOURCE = "submit_resource"
    const val SAVED_ITEMS = "saved_items"
    const val SETTINGS = "settings"
    const val ADMIN_MODERATION = "admin_moderation"
    const val POST_DETAIL = "post_detail/{postId}"
    const val TOOL_DETAIL = "tool_detail/{toolId}"
    const val RESOURCE_DETAIL = "resource_detail/{resourceId}"
    const val CREATOR_PROFILE = "creator/{userId}"
    const val AI_ASSISTANT = "ai_assistant?prompt={prompt}"

    fun postDetail(postId: String) = "post_detail/${Uri.encode(postId)}"
    fun toolDetail(toolId: String) = "tool_detail/${Uri.encode(toolId)}"
    fun resourceDetail(resourceId: String) = "resource_detail/${Uri.encode(resourceId)}"
    fun creatorProfile(userId: String) = "creator/${Uri.encode(userId)}"
    fun aiAssistant(prompt: String? = null): String =
        if (prompt.isNullOrBlank()) "ai_assistant"
        else "ai_assistant?prompt=${Uri.encode(prompt)}"
}

@Composable
fun CreovaNavHost(
    appViewModel: CreovaAppViewModel,
    aiViewModel: AiAssistantViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val strings = LocalCreovaStrings.current
    val snackbarHostState = remember { SnackbarHostState() }
    val uiScope = rememberCoroutineScope()

    val prefs by appViewModel.prefs.collectAsStateWithLifecycle()
    val currentUser by appViewModel.currentUser.collectAsStateWithLifecycle()
    val isAdmin by appViewModel.isAdmin.collectAsStateWithLifecycle()
    val showSignInSheet by appViewModel.showSignInSheet.collectAsStateWithLifecycle()
    val isSigningIn by appViewModel.isSigningIn.collectAsStateWithLifecycle()
    val authMessage by appViewModel.authMessage.collectAsStateWithLifecycle()

    val tools by appViewModel.tools.collectAsStateWithLifecycle()
    val resources by appViewModel.resources.collectAsStateWithLifecycle()
    val posts by appViewModel.posts.collectAsStateWithLifecycle()
    val creators by appViewModel.creators.collectAsStateWithLifecycle()
    val events by appViewModel.engagementEvents.collectAsStateWithLifecycle()
    val savedItems by appViewModel.savedItems.collectAsStateWithLifecycle()
    val savedKeys by appViewModel.savedTargetKeys.collectAsStateWithLifecycle()
    val likedKeys by appViewModel.likedTargetKeys.collectAsStateWithLifecycle()
    val followingIds by appViewModel.followingIds.collectAsStateWithLifecycle()
    val blockedIds by appViewModel.blockedUserIds.collectAsStateWithLifecycle()
    val notifications by appViewModel.notifications.collectAsStateWithLifecycle()
    val openReports by appViewModel.openReports.collectAsStateWithLifecycle()
    val pendingResources by appViewModel.pendingResources.collectAsStateWithLifecycle()

    val searchQuery by appViewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResults by appViewModel.searchResults.collectAsStateWithLifecycle()

    val aiMessages by aiViewModel.messages.collectAsStateWithLifecycle()
    val aiUiState by aiViewModel.uiState.collectAsStateWithLifecycle()

    var pendingToolCategory by remember { mutableStateOf<ToolCategory?>(null) }

    LaunchedEffect(Unit) {
        appViewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val topRoutes = remember { TopTabRoute.entries.map { it.route }.toSet() }
    val showPrimaryNav = currentRoute in topRoutes

    val navigateToTopTab: (TopTabRoute) -> Unit = { tab ->
        if (tab != TopTabRoute.EXPLORE) {
            pendingToolCategory = null
        }
        navController.navigate(tab.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openAiWithAuth: (String?) -> Unit = { initialPrompt ->
        appViewModel.requireAuthOrRun {
            navController.navigate(CreovaRoutes.aiAssistant(initialPrompt))
        }
    }

    val unreadNotificationsCount = remember(notifications) {
        notifications.count { !it.seen }
    }

    LiquidGlassBackground {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 600.dp

            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail && showPrimaryNav) {
                    LiquidGlassNavRail(
                        currentRoute = currentRoute,
                        onSelectTab = navigateToTopTab,
                        onOpenAi = { openAiWithAuth(null) }
                    )
                }

                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (!useRail && showPrimaryNav) {
                            LiquidGlassBottomBar(
                                currentRoute = currentRoute,
                                onSelectTab = navigateToTopTab
                            )
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = CreovaRoutes.HOME,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        composable(CreovaRoutes.HOME) {
                            HomeScreen(
                                language = prefs.language,
                                tools = tools,
                                resources = resources,
                                posts = posts,
                                creators = creators,
                                events = events,
                                savedKeys = savedKeys,
                                unreadNotificationsCount = unreadNotificationsCount,
                                onToggleLanguage = {
                                    val next = if (prefs.language == AppLanguage.AR) AppLanguage.EN else AppLanguage.AR
                                    appViewModel.setLanguage(next)
                                },
                                onOpenSearch = { navController.navigate(CreovaRoutes.SEARCH) },
                                onOpenNotifications = { navController.navigate(CreovaRoutes.NOTIFICATIONS) },
                                onOpenAiAssistant = { openAiWithAuth(null) },
                                onSelectToolCategory = { cat ->
                                    pendingToolCategory = cat
                                    navigateToTopTab(TopTabRoute.EXPLORE)
                                },
                                onOpenTool = { id -> navController.navigate(CreovaRoutes.toolDetail(id)) },
                                onOpenResource = { id -> navController.navigate(CreovaRoutes.resourceDetail(id)) },
                                onOpenPost = { id -> navController.navigate(CreovaRoutes.postDetail(id)) },
                                onOpenCreator = { uid -> navController.navigate(CreovaRoutes.creatorProfile(uid)) },
                                onToggleSaveTool = { tool ->
                                    appViewModel.toggleSave(
                                        targetType = "tool",
                                        targetId = tool.id,
                                        title = if (prefs.language == AppLanguage.AR) tool.nameAr else tool.name,
                                        subtitle = tool.officialUrl
                                    )
                                },
                                onSeeAllTools = { navigateToTopTab(TopTabRoute.EXPLORE) },
                                onSeeAllLibrary = { navigateToTopTab(TopTabRoute.LIBRARY) },
                                onSeeAllCommunity = { navigateToTopTab(TopTabRoute.COMMUNITY) },
                                contentPadding = innerPadding
                            )
                        }

                        composable(CreovaRoutes.EXPLORE) {
                            ExploreToolsScreen(
                                language = prefs.language,
                                tools = tools,
                                savedKeys = savedKeys,
                                initialCategory = pendingToolCategory,
                                onOpenTool = { id -> navController.navigate(CreovaRoutes.toolDetail(id)) },
                                onToggleSaveTool = { tool ->
                                    appViewModel.toggleSave(
                                        targetType = "tool",
                                        targetId = tool.id,
                                        title = if (prefs.language == AppLanguage.AR) tool.nameAr else tool.name,
                                        subtitle = tool.officialUrl
                                    )
                                },
                                contentPadding = innerPadding
                            )
                        }

                        composable(CreovaRoutes.LIBRARY) {
                            LibraryScreen(
                                language = prefs.language,
                                resources = resources,
                                savedKeys = savedKeys,
                                likedKeys = likedKeys,
                                onOpenResource = { id -> navController.navigate(CreovaRoutes.resourceDetail(id)) },
                                onToggleSaveResource = { res ->
                                    appViewModel.toggleSave(
                                        targetType = "resource",
                                        targetId = res.id,
                                        title = if (prefs.language == AppLanguage.AR) res.titleAr else res.title,
                                        subtitle = "${res.fileFormat} • ${res.licenseInfo}"
                                    )
                                },
                                onToggleLikeResource = { res ->
                                    appViewModel.toggleLike(
                                        targetType = "resource",
                                        targetId = res.id,
                                        ownerId = res.creatorId,
                                        title = if (prefs.language == AppLanguage.AR) res.titleAr else res.title
                                    )
                                },
                                onOpenSubmitResource = {
                                    appViewModel.requireAuthOrRun {
                                        navController.navigate(CreovaRoutes.SUBMIT_RESOURCE)
                                    }
                                },
                                contentPadding = innerPadding
                            )
                        }

                        composable(CreovaRoutes.COMMUNITY) {
                            CommunityScreen(
                                language = prefs.language,
                                posts = posts,
                                followingIds = followingIds,
                                likedKeys = likedKeys,
                                savedKeys = savedKeys,
                                currentUserId = currentUser?.uid,
                                onOpenPost = { id -> navController.navigate(CreovaRoutes.postDetail(id)) },
                                onOpenCreator = { uid -> navController.navigate(CreovaRoutes.creatorProfile(uid)) },
                                onCreatePost = {
                                    appViewModel.requireAuthOrRun {
                                        navController.navigate(CreovaRoutes.CREATE_POST)
                                    }
                                },
                                onToggleLikePost = { post ->
                                    appViewModel.toggleLike("post", post.id, post.authorId, post.title)
                                },
                                onToggleSavePost = { post ->
                                    appViewModel.toggleSave("post", post.id, post.title, post.authorName)
                                },
                                onSharePost = { post ->
                                    appViewModel.recordShare("post", post.id)
                                },
                                onEditPost = { post ->
                                    appViewModel.requireAuthOrRun {
                                        navController.navigate("create_post?postId=${Uri.encode(post.id)}")
                                    }
                                },
                                onDeletePost = { post ->
                                    appViewModel.deletePost(post.id)
                                },
                                onReportPost = { post ->
                                    appViewModel.submitReport(
                                        targetType = "post",
                                        targetId = post.id,
                                        reason = "Community guideline report",
                                        confirmationMsg = strings.reportSubmitted
                                    )
                                },
                                onBlockAuthor = { authorId ->
                                    appViewModel.toggleBlockUser(authorId, strings.userBlocked)
                                },
                                contentPadding = innerPadding
                            )
                        }

                        composable(CreovaRoutes.PROFILE) {
                            val myProfile = creators.firstOrNull { it.userId == currentUser?.uid }
                            ProfileScreen(
                                currentUser = currentUser,
                                myProfile = myProfile,
                                isSigningIn = isSigningIn,
                                isAdmin = isAdmin,
                                savedCount = savedItems.size,
                                unreadNotificationsCount = unreadNotificationsCount,
                                onSignInClick = {
                                    appViewModel.triggerGoogleSignIn(context, strings.signInCancelled)
                                },
                                onOpenSavedItems = { navController.navigate(CreovaRoutes.SAVED_ITEMS) },
                                onOpenNotifications = { navController.navigate(CreovaRoutes.NOTIFICATIONS) },
                                onOpenSettings = { navController.navigate(CreovaRoutes.SETTINGS) },
                                onOpenAiAssistant = { openAiWithAuth(null) },
                                onOpenAdminModeration = { navController.navigate(CreovaRoutes.ADMIN_MODERATION) },
                                onUpdateProfile = { name, username, bio, specialty, onDone ->
                                    appViewModel.updateMyProfile(name, username, bio, specialty, onDone)
                                },
                                contentPadding = innerPadding
                            )
                        }

                        composable(CreovaRoutes.SEARCH) {
                            SearchScreen(
                                language = prefs.language,
                                query = searchQuery,
                                recentSearches = prefs.recentSearches,
                                results = searchResults,
                                onQueryChange = appViewModel::updateSearchQuery,
                                onCommitQuery = appViewModel::commitSearchQuery,
                                onClearRecent = appViewModel::clearRecentSearches,
                                onBack = { navController.popBackStack() },
                                onOpenTool = { id -> navController.navigate(CreovaRoutes.toolDetail(id)) },
                                onOpenResource = { id -> navController.navigate(CreovaRoutes.resourceDetail(id)) },
                                onOpenPost = { id -> navController.navigate(CreovaRoutes.postDetail(id)) },
                                onOpenCreator = { uid -> navController.navigate(CreovaRoutes.creatorProfile(uid)) },
                                onSelectCategory = { cat ->
                                    pendingToolCategory = cat
                                    navController.popBackStack()
                                    navigateToTopTab(TopTabRoute.EXPLORE)
                                }
                            )
                        }

                        composable(CreovaRoutes.NOTIFICATIONS) {
                            NotificationsScreen(
                                notifications = notifications,
                                isSignedIn = currentUser != null,
                                onBack = { navController.popBackStack() },
                                onSignInClick = { appViewModel.openSignInSheet() },
                                onMarkAllRead = { appViewModel.markAllNotificationsRead() }
                            )
                        }

                        composable(CreovaRoutes.SAVED_ITEMS) {
                            SavedItemsScreen(
                                savedItems = savedItems,
                                isSignedIn = currentUser != null,
                                onBack = { navController.popBackStack() },
                                onSignInClick = { appViewModel.openSignInSheet() },
                                onOpenTool = { id -> navController.navigate(CreovaRoutes.toolDetail(id)) },
                                onOpenResource = { id -> navController.navigate(CreovaRoutes.resourceDetail(id)) },
                                onOpenPost = { id -> navController.navigate(CreovaRoutes.postDetail(id)) },
                                onRemoveSaved = { item ->
                                    appViewModel.toggleSave(item.targetType, item.targetId, item.title, item.subtitle)
                                }
                            )
                        }

                        composable(CreovaRoutes.SETTINGS) {
                            SettingsScreen(
                                prefs = prefs,
                                isSignedIn = currentUser != null,
                                onBack = { navController.popBackStack() },
                                onSetLanguage = appViewModel::setLanguage,
                                onSetThemeMode = appViewModel::setThemeMode,
                                onSetAiModel = appViewModel::setAiModel,
                                onSetReducedMotion = appViewModel::setReducedMotion,
                                onSignOut = { appViewModel.signOut(context) },
                                onDeleteAccount = {
                                    appViewModel.deleteAccount(context) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(
                            route = "create_post?postId={postId}",
                            arguments = listOf(navArgument("postId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            })
                        ) { entry ->
                            val editingPostId = entry.arguments?.getString("postId")?.let(Uri::decode)
                            val editingPost = posts.firstOrNull { it.id == editingPostId && it.authorId == currentUser?.uid }
                            CreatePostScreen(
                                language = prefs.language,
                                initialPost = editingPost,
                                onBack = { navController.popBackStack() },
                                onPublish = { postId, postType, category, title, description, tags, mediaDataUrl, linkedUrl ->
                                    if (postId == null) {
                                        appViewModel.createPost(
                                            postType = postType,
                                            category = category,
                                            title = title,
                                            description = description,
                                            tags = tags,
                                            mediaDataUrl = mediaDataUrl,
                                            linkedUrl = linkedUrl
                                        ) { navController.popBackStack() }
                                    } else {
                                        appViewModel.updatePost(
                                            postId = postId,
                                            category = category,
                                            title = title,
                                            description = description,
                                            tags = tags,
                                            mediaDataUrl = mediaDataUrl,
                                            linkedUrl = linkedUrl
                                        ) { navController.popBackStack() }
                                    }
                                }
                            )
                        }

                        composable(CreovaRoutes.SUBMIT_RESOURCE) {
                            SubmitResourceScreen(
                                language = prefs.language,
                                onBack = { navController.popBackStack() },
                                onSubmit = { title, titleAr, desc, descAr, cat, tags, fmt, size, license, url, fileUri, mimeType, onProgress, onFinished ->
                                    appViewModel.submitResource(
                                        title = title,
                                        titleAr = titleAr,
                                        description = desc,
                                        descriptionAr = descAr,
                                        category = cat,
                                        tags = tags,
                                        fileFormat = fmt,
                                        fileSizeLabel = size,
                                        licenseInfo = license,
                                        downloadUrl = url,
                                        fileUri = fileUri,
                                        mimeType = mimeType,
                                        onProgress = onProgress,
                                        onFinished = onFinished
                                    ) {
                                        navController.popBackStack()
                                    }
                                }
                            )
                        }

                        composable(CreovaRoutes.ADMIN_MODERATION) {
                            AdminModerationScreen(
                                isAdmin = isAdmin,
                                pendingResources = pendingResources,
                                openReports = openReports,
                                onBack = { navController.popBackStack() },
                                onModerateResource = appViewModel::moderateResource,
                                onUpdateReportStatus = appViewModel::updateReportStatus
                            )
                        }

                        composable(
                            route = CreovaRoutes.TOOL_DETAIL,
                            arguments = listOf(navArgument("toolId") { type = NavType.StringType })
                        ) { entry ->
                            val toolId = Uri.decode(entry.arguments?.getString("toolId").orEmpty())
                            val tool = tools.firstOrNull { it.id == toolId }
                            ToolDetailScreen(
                                language = prefs.language,
                                tool = tool,
                                isSaved = "tool_$toolId" in savedKeys,
                                onBack = { navController.popBackStack() },
                                onToggleSave = { t ->
                                    appViewModel.toggleSave(
                                        targetType = "tool",
                                        targetId = t.id,
                                        title = if (prefs.language == AppLanguage.AR) t.nameAr else t.name,
                                        subtitle = t.officialUrl
                                    )
                                },
                                onToolOpened = { t -> appViewModel.recordToolOpen(t.id) },
                                onAskAiAboutTool = { prompt -> openAiWithAuth(prompt) },
                                onShowError = { msg -> uiScope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }

                        composable(
                            route = CreovaRoutes.RESOURCE_DETAIL,
                            arguments = listOf(navArgument("resourceId") { type = NavType.StringType })
                        ) { entry ->
                            val resourceId = Uri.decode(entry.arguments?.getString("resourceId").orEmpty())
                            val resource = resources.firstOrNull { it.id == resourceId }
                            ResourceDetailScreen(
                                language = prefs.language,
                                resource = resource,
                                isSaved = "resource_$resourceId" in savedKeys,
                                isLiked = "resource_$resourceId" in likedKeys,
                                onBack = { navController.popBackStack() },
                                onToggleSave = { r ->
                                    appViewModel.toggleSave(
                                        targetType = "resource",
                                        targetId = r.id,
                                        title = if (prefs.language == AppLanguage.AR) r.titleAr else r.title,
                                        subtitle = "${r.fileFormat} • ${r.licenseInfo}"
                                    )
                                },
                                onToggleLike = { r ->
                                    appViewModel.toggleLike(
                                        targetType = "resource",
                                        targetId = r.id,
                                        ownerId = r.creatorId,
                                        title = if (prefs.language == AppLanguage.AR) r.titleAr else r.title
                                    )
                                },
                                onReportResource = { r ->
                                    appViewModel.submitReport(
                                        targetType = "resource",
                                        targetId = r.id,
                                        reason = "Resource link or license report",
                                        confirmationMsg = strings.reportSubmitted
                                    )
                                },
                                onResourceViewed = { r -> appViewModel.recordResourceView(r.id) },
                                onShowError = { msg -> uiScope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }

                        composable(
                            route = CreovaRoutes.POST_DETAIL,
                            arguments = listOf(navArgument("postId") { type = NavType.StringType }),
                            deepLinks = listOf(navDeepLink { uriPattern = "creova://post/{postId}" })
                        ) { entry ->
                            val postId = Uri.decode(entry.arguments?.getString("postId").orEmpty())
                            val post = posts.firstOrNull { it.id == postId }
                            val comments by remember(postId) {
                                appViewModel.observeCommentsForPost(postId)
                            }.collectAsStateWithLifecycle(initialValue = emptyList())

                            PostDetailScreen(
                                language = prefs.language,
                                post = post,
                                comments = comments,
                                isLiked = "post_$postId" in likedKeys,
                                isSaved = "post_$postId" in savedKeys,
                                isFollowingAuthor = post != null && post.authorId in followingIds,
                                currentUserId = currentUser?.uid,
                                onBack = { navController.popBackStack() },
                                onOpenCreator = { uid -> navController.navigate(CreovaRoutes.creatorProfile(uid)) },
                                onToggleLike = { p -> appViewModel.toggleLike("post", p.id, p.authorId, p.title) },
                                onToggleSave = { p -> appViewModel.toggleSave("post", p.id, p.title, p.authorName) },
                                onToggleFollow = { uid -> appViewModel.toggleFollow(uid) },
                                onSendComment = appViewModel::addComment,
                                onShowError = { msg -> uiScope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }

                        composable(
                            route = CreovaRoutes.CREATOR_PROFILE,
                            arguments = listOf(navArgument("userId") { type = NavType.StringType })
                        ) { entry ->
                            val userId = Uri.decode(entry.arguments?.getString("userId").orEmpty())
                            val creator = creators.firstOrNull { it.userId == userId }
                            CreatorProfileScreen(
                                language = prefs.language,
                                creator = creator,
                                posts = posts,
                                resources = resources,
                                currentUserId = currentUser?.uid,
                                isFollowing = userId in followingIds,
                                isBlocked = userId in blockedIds,
                                onBack = { navController.popBackStack() },
                                onToggleFollow = { uid -> appViewModel.toggleFollow(uid) },
                                onToggleBlock = { uid -> appViewModel.toggleBlockUser(uid, strings.userBlocked) },
                                onReportCreator = { uid ->
                                    appViewModel.submitReport(
                                        targetType = "user",
                                        targetId = uid,
                                        reason = "Creator profile report",
                                        confirmationMsg = strings.reportSubmitted
                                    )
                                },
                                onOpenPost = { id -> navController.navigate(CreovaRoutes.postDetail(id)) },
                                onOpenResource = { id -> navController.navigate(CreovaRoutes.resourceDetail(id)) }
                            )
                        }

                        composable(
                            route = CreovaRoutes.AI_ASSISTANT,
                            arguments = listOf(
                                navArgument("prompt") {
                                    type = NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { entry ->
                            val initialPrompt = entry.arguments?.getString("prompt")?.let { Uri.decode(it) }
                            AiAssistantScreen(
                                language = prefs.language,
                                modelOption = prefs.aiModel,
                                messages = aiMessages,
                                uiState = aiUiState,
                                tools = tools,
                                resources = resources,
                                initialPrompt = initialPrompt,
                                onBack = { navController.popBackStack() },
                                onSendPrompt = { prompt, structured ->
                                    aiViewModel.sendChatPrompt(
                                        prompt = prompt,
                                        modelOption = prefs.aiModel,
                                        tools = tools,
                                        resources = resources,
                                        requestStructured = structured
                                    )
                                },
                                onClearChat = aiViewModel::clearConversation,
                                onSelectModel = appViewModel::setAiModel,
                                onShowError = { msg -> uiScope.launch { snackbarHostState.showSnackbar(msg) } }
                            )
                        }
                    }
                }
            }
        }

        if (showSignInSheet) {
            GoogleSignInSheet(
                isLoading = isSigningIn,
                errorMessage = authMessage,
                onDismiss = appViewModel::dismissSignInSheet,
                onSignInClick = {
                    appViewModel.triggerGoogleSignIn(context, strings.signInCancelled)
                }
            )
        }
    }
}
