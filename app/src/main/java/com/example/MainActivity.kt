package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.data.model.GameMode
import com.example.data.model.MatchState
import com.example.data.repository.CricketCloudRepository
import com.example.data.repository.LocalPreferencesRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.screens.HomeHubScreen
import com.example.ui.screens.LiveMatchScreen
import com.example.ui.screens.MatchSetupScreen
import com.example.ui.screens.OnlineLobbyScreen
import com.example.ui.screens.OnlineRoomMatchHost
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.TournamentsAndCareerScreen
import com.example.ui.theme.RealCricketChampionsTheme
import com.example.viewmodel.MainViewModel
import com.example.viewmodel.MatchViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object AppRoutes {
    const val HUB = "hub"
    const val SETUP = "setup"
    const val MATCH = "match"
    const val ONLINE_ROOM = "online_room/{roomId}"
    fun onlineRoom(roomId: String) = "online_room/$roomId"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RealCricketChampionsTheme {
                RealCricketAppRoot()
            }
        }
    }
}

@Composable
fun RealCricketAppRoot(auth: FirebaseAuth = Firebase.auth) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    var isGuestMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = { isGuestMode = false },
            onUnauthenticated = {},
            scope = scope
        )
    }

    val user = currentUser
    if (user == null && !isGuestMode) {
        AuthScreen(
            onAuthSuccess = { isGuestMode = false },
            onContinueAsGuest = { isGuestMode = true }
        )
    } else {
        val sessionKey = user?.uid ?: "guest_offline"
        key(sessionKey) {
            val mainViewModel: MainViewModel = viewModel(
                key = sessionKey,
                factory = viewModelFactory {
                    initializer {
                        val app = checkNotNull(this[APPLICATION_KEY])
                        val cloudRepo = if (user != null) {
                            val dbId = app.getString(R.string.firestore_database_id)
                            CricketCloudRepository(FirebaseFirestore.getInstance(dbId), auth)
                        } else null
                        MainViewModel(
                            cloudRepository = cloudRepo,
                            localPrefs = LocalPreferencesRepository(app.applicationContext),
                            currentUserId = user?.uid,
                            userDisplayName = user?.displayName?.takeIf { it.isNotBlank() } ?: "Guest Captain"
                        )
                    }
                }
            )
            AuthenticatedAppShell(
                mainViewModel = mainViewModel,
                onRequestGoogleSignIn = { isGuestMode = false },
                onSignOut = {
                    signOut(credentialManager, scope) {
                        isGuestMode = false
                    }
                }
            )
        }
    }
}

@Composable
private fun AuthenticatedAppShell(
    mainViewModel: MainViewModel,
    onRequestGoogleSignIn: () -> Unit,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    var activeTab by remember { mutableStateOf(0) }
    var activeMatch by remember { mutableStateOf<MatchState?>(null) }

    val settings by mainViewModel.settings.collectAsStateWithLifecycle()
    val localCareer by mainViewModel.localCareer.collectAsStateWithLifecycle()
    val userProfileState by mainViewModel.userProfileState.collectAsStateWithLifecycle()
    val matchHistoryState by mainViewModel.matchHistoryState.collectAsStateWithLifecycle()
    val leaderboardState by mainViewModel.leaderboardState.collectAsStateWithLifecycle()
    val waitingRoomsState by mainViewModel.waitingRoomsState.collectAsStateWithLifecycle()
    val myRoomsState by mainViewModel.myRoomsState.collectAsStateWithLifecycle()
    val offlineHistory by mainViewModel.offlineMatchHistory.collectAsStateWithLifecycle()
    val setupConfig by mainViewModel.setupConfig.collectAsStateWithLifecycle()
    val worldCup by mainViewModel.worldCupState.collectAsStateWithLifecycle()
    val franchise by mainViewModel.franchiseState.collectAsStateWithLifecycle()
    val offlineCup by mainViewModel.offlineCupState.collectAsStateWithLifecycle()
    val banner by mainViewModel.statusBanner.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(banner) {
        val msg = banner
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            mainViewModel.clearBanner()
        }
    }

    NavHost(navController = navController, startDestination = AppRoutes.HUB) {
        composable(AppRoutes.HUB) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useRail = maxWidth >= 680.dp
                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar {
                                HubBottomBarItems(activeTab = activeTab, onSelect = { activeTab = it })
                            }
                        }
                    }
                ) { padding ->
                    Row(modifier = Modifier.fillMaxSize().padding(padding)) {
                        if (useRail) {
                            NavigationRail(modifier = Modifier.fillMaxHeight()) {
                                HubRailItems(activeTab = activeTab, onSelect = { activeTab = it })
                            }
                        }
                        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
                            when (activeTab) {
                                0 -> HomeHubScreen(
                                    displayName = mainViewModel.userDisplayName,
                                    isAuthenticated = mainViewModel.isAuthenticated,
                                    cloudProfile = (userProfileState as? com.example.viewmodel.UiState.Success)?.data,
                                    localCareer = localCareer,
                                    onSelectMode = { mode ->
                                        when (mode) {
                                            GameMode.ONLINE -> activeTab = 2
                                            GameMode.WORLD_CUP, GameMode.FRANCHISE, GameMode.TOURNAMENT, GameMode.CAREER -> activeTab = 1
                                            else -> {
                                                mainViewModel.startModeSetup(mode)
                                                navController.navigate(AppRoutes.SETUP)
                                            }
                                        }
                                    },
                                    onRequestGoogleSignIn = onRequestGoogleSignIn
                                )
                                1 -> TournamentsAndCareerScreen(
                                    worldCupState = worldCup,
                                    franchiseState = franchise,
                                    offlineCupState = offlineCup,
                                    careerProfile = localCareer,
                                    onPlayFixture = { mode, fixture ->
                                        mainViewModel.startTournamentFixtureSetup(mode, fixture)
                                        navController.navigate(AppRoutes.SETUP)
                                    },
                                    onSimulateAi = { mode -> mainViewModel.simulateAiTournamentFixtures(mode) },
                                    onResetTournament = { mode, teamId -> mainViewModel.resetTournament(mode, teamId) },
                                    onUpgradeCareerSkill = { skill -> mainViewModel.upgradeCareerSkill(skill) },
                                    onPlayCareerMatch = {
                                        mainViewModel.startModeSetup(GameMode.CAREER)
                                        navController.navigate(AppRoutes.SETUP)
                                    }
                                )
                                2 -> OnlineLobbyScreen(
                                    isAuthenticated = mainViewModel.isAuthenticated,
                                    currentUserId = mainViewModel.currentUserId,
                                    preferredTeamId = settings.preferredTeamId,
                                    waitingRoomsState = waitingRoomsState,
                                    myRoomsState = myRoomsState,
                                    onCreateRoom = { fmt, overs, stadiumId, teamId ->
                                        mainViewModel.createOnlineRoom(fmt, overs, stadiumId, teamId) { roomId ->
                                            navController.navigate(AppRoutes.onlineRoom(roomId))
                                        }
                                    },
                                    onJoinRoomById = { roomId, teamId ->
                                        mainViewModel.joinRoomById(roomId, teamId) { id ->
                                            navController.navigate(AppRoutes.onlineRoom(id))
                                        }
                                    },
                                    onJoinRoomByCode = { code, teamId ->
                                        mainViewModel.joinRoomByCode(code, teamId) { id ->
                                            navController.navigate(AppRoutes.onlineRoom(id))
                                        }
                                    },
                                    onEnterActiveRoom = { room ->
                                        navController.navigate(AppRoutes.onlineRoom(room.id))
                                    },
                                    onRequestSignIn = onRequestGoogleSignIn
                                )
                                else -> ProfileAndSettingsScreen(
                                    isAuthenticated = mainViewModel.isAuthenticated,
                                    displayName = mainViewModel.userDisplayName,
                                    userProfileState = userProfileState,
                                    localCareer = localCareer,
                                    leaderboardState = leaderboardState,
                                    matchHistoryState = matchHistoryState,
                                    offlineMatchHistory = offlineHistory,
                                    settings = settings,
                                    onUpdateSettings = { mainViewModel.updateSettings(it) },
                                    onSignIn = onRequestGoogleSignIn,
                                    onSignOut = onSignOut
                                )
                            }
                        }
                    }
                }
            }
        }

        composable(AppRoutes.SETUP) {
            Scaffold(contentWindowInsets = WindowInsets.safeDrawing) { padding ->
                Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                    MatchSetupScreen(
                        config = setupConfig,
                        onUpdateConfig = { mainViewModel.updateSetup(it) },
                        onFlipCoin = { heads -> mainViewModel.performCoinToss(heads) },
                        onChooseTossDecision = { batFirst -> mainViewModel.setTossDecision(batFirst) },
                        onLaunchMatch = {
                            activeMatch = mainViewModel.createMatchFromSetup()
                            navController.navigate(AppRoutes.MATCH)
                        },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }

        composable(AppRoutes.MATCH) {
            val match = activeMatch
            if (match != null) {
                val matchVm = remember(match.matchId) {
                    MatchViewModel(
                        initialMatch = match,
                        onMatchFinishedCallback = { finished -> mainViewModel.onMatchCompleted(finished) }
                    )
                }
                LiveMatchScreen(
                    matchViewModel = matchVm,
                    graphicsQuality = settings.graphicsQuality,
                    hapticsEnabled = settings.hapticsEnabled,
                    onChangeQuality = { q -> mainViewModel.updateSettings { it.copy(graphicsQuality = q) } },
                    onExitMatch = {
                        activeMatch = null
                        navController.popBackStack(AppRoutes.HUB, inclusive = false)
                    }
                )
            }
        }

        composable(AppRoutes.ONLINE_ROOM) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            OnlineRoomMatchHost(
                roomId = roomId,
                mainViewModel = mainViewModel,
                graphicsQuality = settings.graphicsQuality,
                hapticsEnabled = settings.hapticsEnabled,
                onChangeQuality = { q -> mainViewModel.updateSettings { it.copy(graphicsQuality = q) } },
                onExitRoom = { navController.popBackStack(AppRoutes.HUB, inclusive = false) }
            )
        }
    }
}

@Composable
private fun HubRailItems(
    activeTab: Int,
    onSelect: (Int) -> Unit
) {
    val items = listOf(
        Triple("Play", Icons.Default.SportsCricket, "nav_play"),
        Triple("Tournaments", Icons.Default.EmojiEvents, "nav_tournaments"),
        Triple("Online", Icons.Default.Public, "nav_online"),
        Triple("Profile", Icons.Default.Person, "nav_profile")
    )
    items.forEachIndexed { idx, (label, icon, tag) ->
        NavigationRailItem(
            selected = activeTab == idx,
            onClick = { onSelect(idx) },
            icon = { Icon(icon, contentDescription = label) },
            label = { Text(label) },
            modifier = Modifier.testTag(tag)
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.HubBottomBarItems(
    activeTab: Int,
    onSelect: (Int) -> Unit
) {
    val items = listOf(
        Triple("Play", Icons.Default.SportsCricket, "nav_play"),
        Triple("Tournaments", Icons.Default.EmojiEvents, "nav_tournaments"),
        Triple("Online", Icons.Default.Public, "nav_online"),
        Triple("Profile", Icons.Default.Person, "nav_profile")
    )
    items.forEachIndexed { idx, (label, icon, tag) ->
        NavigationBarItem(
            selected = activeTab == idx,
            onClick = { onSelect(idx) },
            icon = { Icon(icon, contentDescription = label) },
            label = { Text(label) },
            modifier = Modifier.testTag(tag)
        )
    }
}
