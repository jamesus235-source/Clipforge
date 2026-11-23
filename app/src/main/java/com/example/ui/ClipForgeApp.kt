package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.Output
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.MovieFilter
import androidx.compose.material.icons.outlined.Output
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.screens.CreateScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EditorScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.theme.CharcoalPanel
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.ObsidianCanvas
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MainViewModel
import kotlinx.serialization.Serializable

@Serializable
object RouteCreate

@Serializable
object RouteEditor

@Serializable
object RouteExport

@Serializable
object RouteDashboard

private data class NavTabItem<T : Any>(
    val route: T,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val tag: String
)

@Composable
fun ClipForgeApp(
    viewModel: MainViewModel = viewModel()
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val createState by viewModel.createState.collectAsStateWithLifecycle()
    val activeProject by viewModel.activeProject.collectAsStateWithLifecycle()
    val selectedClip by viewModel.selectedClip.collectAsStateWithLifecycle()
    val exportState by viewModel.exportState.collectAsStateWithLifecycle()
    val projects by viewModel.projects.collectAsStateWithLifecycle()
    val brandPresets by viewModel.brandPresets.collectAsStateWithLifecycle()
    val aiBusyMessage by viewModel.aiBusyMessage.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    LaunchedEffect(snackbarMessage) {
        val msg = snackbarMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeSnackbar()
        }
    }

    val navItems = remember {
        listOf(
            NavTabItem(
                route = RouteCreate,
                labelRes = R.string.nav_create,
                selectedIcon = Icons.Filled.AddCircle,
                unselectedIcon = Icons.Outlined.AddCircleOutline,
                tag = "nav_create"
            ),
            NavTabItem(
                route = RouteEditor,
                labelRes = R.string.nav_editor,
                selectedIcon = Icons.Filled.MovieFilter,
                unselectedIcon = Icons.Outlined.MovieFilter,
                tag = "nav_editor"
            ),
            NavTabItem(
                route = RouteExport,
                labelRes = R.string.nav_export,
                selectedIcon = Icons.Filled.Output,
                unselectedIcon = Icons.Outlined.Output,
                tag = "nav_export"
            ),
            NavTabItem(
                route = RouteDashboard,
                labelRes = R.string.nav_projects,
                selectedIcon = Icons.Filled.Folder,
                unselectedIcon = Icons.Outlined.Folder,
                tag = "nav_projects"
            )
        )
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    fun navigateSingleTop(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = ObsidianCanvas,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(containerColor = CharcoalPanel) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            val label = stringResource(item.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateSingleTop(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ElectricViolet,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = ElectricViolet.copy(alpha = 0.18f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(containerColor = CharcoalPanel) {
                        navItems.forEach { item ->
                            val selected = currentDestination?.hasRoute(item.route::class) == true
                            val label = stringResource(item.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateSingleTop(item.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ElectricViolet,
                                    selectedTextColor = TextPrimary,
                                    indicatorColor = ElectricViolet.copy(alpha = 0.18f),
                                    unselectedIconColor = TextSecondary,
                                    unselectedTextColor = TextSecondary
                                ),
                                modifier = Modifier.testTag(item.tag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = RouteCreate
                    ) {
                        composable<RouteCreate> {
                            CreateScreen(
                                state = createState,
                                aiNotice = viewModel.aiStatusNotice,
                                onYouTubeUrlChanged = viewModel::onYouTubeUrlChanged,
                                onFetchYouTubeMetadata = viewModel::fetchYouTubeMetadata,
                                onPickLocalVideo = viewModel::importLocalVideoUri,
                                onLoadSampleVideo = viewModel::loadSampleStudioVideo,
                                onClipCountChanged = viewModel::updateClipCount,
                                onPreferredDurationChanged = viewModel::updatePreferredDuration,
                                onToggleManualRange = viewModel::toggleManualRange,
                                onManualRangeChanged = viewModel::updateManualRange,
                                onGenerateClips = {
                                    viewModel.generateClips { navigateSingleTop(RouteEditor) }
                                },
                                onOpenProjectsTab = { navigateSingleTop(RouteDashboard) }
                            )
                        }
                        composable<RouteEditor> {
                            EditorScreen(
                                project = activeProject,
                                selectedClip = selectedClip,
                                brandPresets = brandPresets,
                                aiNotice = viewModel.aiStatusNotice,
                                aiBusyMessage = aiBusyMessage,
                                onSelectClip = viewModel::selectClip,
                                onAddManualClip = viewModel::addManualClip,
                                onUpdateClip = viewModel::updateSelectedClip,
                                onApplyPreset = viewModel::applySubtitlePreset,
                                onUpdateSubtitleStyle = viewModel::updateSubtitleStyle,
                                onUpdateFraming = viewModel::updateFraming,
                                onUpdateSegmentText = viewModel::updateSubtitleSegmentText,
                                onUpdateSegmentTiming = viewModel::updateSubtitleSegmentTiming,
                                onAddSubtitleSegment = viewModel::addSubtitleSegment,
                                onPickBackgroundMusic = viewModel::pickBackgroundMusicUri,
                                onRegenerateClip = viewModel::regenerateSelectedClip,
                                onRefreshHooks = viewModel::refreshAiHooks,
                                onRefreshTitles = viewModel::refreshAiTitles,
                                onRefreshSocialCaption = viewModel::refreshAiSocialCaption,
                                onSaveBrandPreset = viewModel::saveCurrentStyleAsBrandPreset,
                                onApplyBrandPreset = viewModel::applyBrandPreset,
                                onNavigateToExport = { navigateSingleTop(RouteExport) },
                                onNavigateToCreate = { navigateSingleTop(RouteCreate) }
                            )
                        }
                        composable<RouteExport> {
                            ExportScreen(
                                project = activeProject,
                                exportState = exportState,
                                onUpdateConfig = viewModel::updateExportConfig,
                                onExportSingleClip = viewModel::exportSingleClip,
                                onExportAllAndZip = viewModel::exportAllClipsAndZip,
                                onCancelExport = viewModel::cancelExport,
                                onSaveToGallery = viewModel::saveExportedClipToGallery,
                                onNavigateToCreate = { navigateSingleTop(RouteCreate) }
                            )
                        }
                        composable<RouteDashboard> {
                            DashboardScreen(
                                projects = projects,
                                brandPresets = brandPresets,
                                onOpenProject = { proj ->
                                    viewModel.openProject(proj) { navigateSingleTop(RouteEditor) }
                                },
                                onRenameProject = viewModel::renameProject,
                                onDeleteProject = viewModel::deleteProject,
                                onSaveClipToGallery = viewModel::saveExportedClipToGallery,
                                onDeleteBrandPreset = viewModel::deleteBrandPreset,
                                onCreateNewProject = { navigateSingleTop(RouteCreate) }
                            )
                        }
                    }
                }
            }
        }
    }
}
