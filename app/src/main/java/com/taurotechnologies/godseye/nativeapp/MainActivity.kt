package com.taurotechnologies.godseye.nativeapp

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.taurotechnologies.godseye.nativeapp.auth.GoogleSignIn
import com.taurotechnologies.godseye.nativeapp.auth.GoogleSignInResult
import com.taurotechnologies.godseye.nativeapp.data.AuthRepository
import com.taurotechnologies.godseye.nativeapp.data.ExploreStore
import com.taurotechnologies.godseye.nativeapp.data.explorePanel
import com.taurotechnologies.godseye.nativeapp.notifications.AlertInboxStore
import com.taurotechnologies.godseye.nativeapp.notifications.AlertsBell
import com.taurotechnologies.godseye.nativeapp.notifications.AlertsOverlay
import com.taurotechnologies.godseye.nativeapp.notifications.AlertsViewModel
import com.taurotechnologies.godseye.nativeapp.notifications.NavigationRequest
import com.taurotechnologies.godseye.nativeapp.notifications.ensureAlertChannel
import com.taurotechnologies.godseye.nativeapp.ui.home.ProvideOpenAlerts
import com.taurotechnologies.godseye.nativeapp.ui.screens.ExplorePanelScreen
import com.taurotechnologies.godseye.nativeapp.ui.screens.ExploreScreen
import com.taurotechnologies.godseye.nativeapp.ui.screens.LoginScreen
import com.taurotechnologies.godseye.nativeapp.ui.screens.RadarScreen
import com.taurotechnologies.godseye.nativeapp.ui.screens.SettingsScreen
import com.taurotechnologies.godseye.nativeapp.ui.screens.ThemeScreen
import androidx.compose.foundation.isSystemInDarkTheme
import com.taurotechnologies.godseye.nativeapp.ui.theme.ColorStyle
import com.taurotechnologies.godseye.nativeapp.ui.theme.SuvidhaRadarTheme
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collectLatest
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    private val model: AppViewModel by viewModels()

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(com.taurotechnologies.godseye.nativeapp.settings.withAppLanguage(newBase))
    }

    private val authRepository: AuthRepository by lazy { AuthRepository(applicationContext) }
    private val googleSignIn: GoogleSignIn by lazy { GoogleSignIn(applicationContext) }
    private val alerts: AlertsViewModel by viewModels()
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        refreshNotificationPermission()
    }

    private var authRefreshJob: kotlinx.coroutines.Job? = null

    override fun onStop() {
        authRefreshJob?.cancel()
        authRefreshJob = null
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        authRefreshJob?.cancel()
        authRefreshJob = lifecycleScope.launch {
            authRepository.restore()
            // Keep the access token fresh while the app is open; flights need a valid JWT.
            while (isActive) {
                delay(30_000L)
                authRepository.refreshIfExpiring()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Recovery path: the user may have allowed notifications in Android settings and came back.
        refreshNotificationPermission()
    }

    /** Warm start: a notification tap or godseye:// link arrives while the activity is already running (singleTop). */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLaunchIntent(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ensureAlertChannel(this)
        refreshNotificationPermission()
        // Cold start: the launch intent may be a notification tap or a link. Touching `model` first initializes the catalog.
        handleLaunchIntent(intent)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                model.nativePreferences.map { it.refreshMinutes }.distinctUntilChanged().collectLatest { minutes ->
                    if (minutes > 0) while (isActive) {
                        delay(minutes * 60_000L)
                        if (!model.liveRadar.value.loading) model.refreshLiveRadar()
                    }
                }
            }
        }
        setContent {
            val authState by authRepository.state.collectAsStateWithLifecycle()
            val theme: ThemeChoice by model.theme.collectAsStateWithLifecycle()
            val style: ColorStyle by model.colorStyle.collectAsStateWithLifecycle()
            val preferences by model.nativePreferences.collectAsStateWithLifecycle()
            val systemDensity = LocalDensity.current
            val isDark: Boolean = when (theme) {
                ThemeChoice.SYSTEM -> isSystemInDarkTheme()
                ThemeChoice.LIGHT -> false
                ThemeChoice.DARK -> true
            }
            CompositionLocalProvider(LocalDensity provides Density(systemDensity.density, systemDensity.fontScale * preferences.textScale)) {
            SuvidhaRadarTheme(darkTheme = isDark, style = style, monoFont = preferences.monoFont) {
                var guestMode by remember { mutableStateOf(false) }
                var googleError by remember { mutableStateOf<String?>(null) }
                var googleInfo by remember { mutableStateOf<String?>(null) }
                var googleInProgress by remember { mutableStateOf(false) }
                val scope = rememberCoroutineScope()
                val session = authState.session
                LaunchedEffect(session?.accessToken) {
                    model.setApiSession(session?.accessToken)
                }
                if (session == null && !guestMode) {
                    LoginScreen(
                        isLoading = authState.isLoading || googleInProgress,
                        error = authState.error ?: googleError,
                        info = authState.info ?: googleInfo,
                        onSubmit = { email, password, createAccount ->
                            googleError = null
                            googleInfo = null
                            scope.launch {
                                if (createAccount) authRepository.signUp(email, password)
                                else authRepository.signIn(email, password)
                            }
                        },
                        onContinueWithGoogle = {
                            googleError = null
                            googleInfo = null
                            googleInProgress = true
                            scope.launch {
                                try {
                                    when (val result = googleSignIn.signIn(this@MainActivity)) {
                                        is GoogleSignInResult.Success -> authRepository.signInWithGoogle(result.idToken, result.nonce)
                                        is GoogleSignInResult.Failure -> googleError = result.message
                                        GoogleSignInResult.Cancelled -> googleInfo = "Google sign-in was canceled. You can try again anytime."
                                    }
                                } finally {
                                    googleInProgress = false
                                }
                            }
                        },
                        onContinueAsGuest = { guestMode = true },
                    )
                } else {
                    SuvidhaRadarApp(
                        model = model,
                        alerts = alerts,
                        accountEmail = session?.email,
                        onSignOut = {
                            guestMode = false
                            scope.launch { authRepository.signOut() }
                        },
                        onRequestNotifications = ::requestNotificationPermission,
                        onOpenNotificationSettings = ::openNotificationSettings,
                    )
                }
            }
            }
        }
    }

    private fun refreshNotificationPermission() {
        val required = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
        val granted = !required ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        alerts.refreshPermission(
            runtimePermissionRequired = required,
            granted = granted,
            shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS),
        )
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        alerts.markPermissionAsked()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    private fun openNotificationSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        } catch (_: ActivityNotFoundException) {
            // No settings screen to open on this device.
        }
    }

    /** Applies a godseye:// link or a notification tap. Overlays are updated in [AlertsViewModel]; panels go through the model. */
    private fun handleLaunchIntent(launch: Intent?) {
        val request = alerts.routeIntent(launch, System.currentTimeMillis()) { explorePanel(it) != null } ?: return
        if (request is NavigationRequest.OpenPanel) model.openExplorePanel(request.panelId)
    }
}

/** Routes between the radar, settings and Explore. */
@Composable
fun SuvidhaRadarApp(
    model: AppViewModel,
    alerts: AlertsViewModel,
    accountEmail: String?,
    onSignOut: () -> Unit,
    onRequestNotifications: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
) {
    val screen: Screen by model.screen.collectAsStateWithLifecycle()
    val theme: ThemeChoice by model.theme.collectAsStateWithLifecycle()
    val radarStateHolder = rememberSaveableStateHolder()
    val context = LocalContext.current
    val exploreSaved: Set<String> by ExploreStore.saved.collectAsStateWithLifecycle()
    val nativePrefs by model.nativePreferences.collectAsStateWithLifecycle()
    val explorePanelId: String? by model.explorePanelId.collectAsStateWithLifecycle()
    Box(modifier = Modifier.fillMaxSize()) {
        // The home top bar draws the alerts bell itself, through this opener.
        ProvideOpenAlerts(open = alerts::openList) { when (screen) {
            Screen.RADAR -> radarStateHolder.SaveableStateProvider(Screen.RADAR.name) { RadarScreen(model) }
            Screen.SETTINGS -> SettingsScreen(theme, model, accountEmail, onSignOut)
            Screen.THEME -> ThemeScreen(theme, model.colorStyle.collectAsStateWithLifecycle().value, model)
            Screen.EXPLORE -> ExploreScreen(saved = exploreSaved, isVisible = { id, category -> nativePrefs.isExplorePanelVisible(id, category) }, onBack = model::closeExplore, onToggleSave = { id -> ExploreStore.toggle(context, id) })
            Screen.PANEL -> ExplorePanelScreen(panelId = explorePanelId.orEmpty(), saved = exploreSaved.contains(explorePanelId), onBack = model::closeExplorePanel, onToggleSave = { id -> ExploreStore.toggle(context, id) })
        } }
        val inbox by AlertInboxStore.state.collectAsStateWithLifecycle()
        // The radar home puts the bell in its own top bar, so it is not floated over that screen.
        // Settings (and its Theme page) has a search field in the header instead of the bell.
        if (screen != Screen.RADAR && screen != Screen.SETTINGS && screen != Screen.THEME) {
            AlertsBell(
                unread = inbox.unreadCount(System.currentTimeMillis()),
                onClick = alerts::openList,
                modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp),
            )
        }
        // Last in the Box so its Back handler is registered after the screens' handlers and wins while open.
        AlertsOverlay(
            alerts = alerts,
            isPanelKnown = { explorePanel(it) != null },
            onOpenPanel = { id -> alerts.closeAll(); model.openExplorePanel(id) },
            onRequestPermission = onRequestNotifications,
            onOpenNotificationSettings = onOpenNotificationSettings,
        )
    }
}
