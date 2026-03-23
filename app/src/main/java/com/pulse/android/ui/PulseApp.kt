package com.pulse.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pulse.android.ui.components.NowPlayingBar
import com.pulse.android.ui.screens.AlbumsScreen
import com.pulse.android.ui.screens.CloudScreen
import com.pulse.android.ui.screens.DashboardScreen
import com.pulse.android.ui.screens.NowPlayingScreen
import com.pulse.android.ui.screens.SettingsScreen
import com.pulse.android.ui.theme.DarkPulseColors
import com.pulse.android.ui.theme.LightPulseColors
import com.pulse.android.ui.theme.LocalPulseColors
import com.pulse.android.ui.theme.PulseColors
import com.pulse.android.ui.theme.PulseTheme
import com.pulse.android.viewmodel.PlayerViewModel

sealed class Screen(val route: String) {
    object Artists  : Screen("artists")
    object Albums   : Screen("albums")
    object Cloud    : Screen("cloud")
    object Settings : Screen("settings")
}

@Composable
fun PulseApp() {
    val vm: PlayerViewModel = viewModel()
    val isDark by vm.isDarkTheme.collectAsState()
    val colors = if (isDark) DarkPulseColors else LightPulseColors

    CompositionLocalProvider(LocalPulseColors provides colors) {
        PulseTheme(isDark = isDark) {
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            val isNowPlayingRoute = currentRoute == "nowplaying"

            Scaffold(
                contentWindowInsets = WindowInsets(0),
                containerColor = colors.bg,
                bottomBar = {
                    if (!isNowPlayingRoute) {
                        NowPlayingBar(vm = vm, onClick = { navController.navigate("nowplaying") })
                    }
                }
            ) { padding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    if (!isNowPlayingRoute) {
                        PulseSidebar(
                            currentRoute = currentRoute,
                            colors = colors,
                            navController = navController
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .width(1.dp)
                                .background(colors.border)
                        )
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Artists.route,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    ) {
                        composable(Screen.Artists.route) {
                            DashboardScreen(vm = vm, navController = navController)
                        }
                        composable(Screen.Albums.route) {
                            AlbumsScreen(vm = vm, navController = navController)
                        }
                        composable(Screen.Cloud.route) {
                            CloudScreen(vm = vm, navController = navController, startPrefix = "Music/")
                        }
                        composable(
                            route = "cloud_prefix?p={p}",
                            arguments = listOf(navArgument("p") {
                                type = NavType.StringType
                                defaultValue = "Music/"
                            })
                        ) { backStack ->
                            val prefix = backStack.arguments?.getString("p") ?: "Music/"
                            CloudScreen(vm = vm, navController = navController, startPrefix = prefix)
                        }
                        composable(Screen.Settings.route) {
                            SettingsScreen(vm = vm)
                        }
                        composable("nowplaying") {
                            NowPlayingScreen(vm = vm, onBack = {
                                if (!navController.popBackStack()) {
                                    navController.navigate(Screen.Artists.route)
                                }
                            })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PulseSidebar(
    currentRoute: String?,
    colors: PulseColors,
    navController: NavController,
) {
    Column(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight()
            .background(colors.surface)
            .padding(vertical = 12.dp)
    ) {
        // App name
        Text(
            "Pulse",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.green,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(Modifier.height(4.dp))

        // LIBRARY
        SidebarSectionLabel("LIBRARY", colors)
        SidebarItem(
            label = "Artists",
            icon = Icons.Default.Person,
            selected = currentRoute == Screen.Artists.route,
            colors = colors,
        ) {
            navController.navigate(Screen.Artists.route) {
                popUpTo(Screen.Artists.route) { inclusive = true }
                launchSingleTop = true
            }
        }
        SidebarItem(
            label = "Albums",
            icon = Icons.Default.Album,
            selected = currentRoute == Screen.Albums.route,
            colors = colors,
        ) {
            navController.navigate(Screen.Albums.route) {
                popUpTo(Screen.Artists.route) { inclusive = false }
                launchSingleTop = true
            }
        }

        Spacer(Modifier.height(8.dp))
        HorizontalDivider(
            color = colors.border,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(8.dp))

        // BROWSE
        SidebarSectionLabel("BROWSE", colors)
        SidebarItem(
            label = "Cloud",
            icon = Icons.Default.Cloud,
            selected = currentRoute == Screen.Cloud.route || currentRoute == "cloud_prefix?p={p}",
            colors = colors,
        ) {
            navController.navigate(Screen.Cloud.route) {
                popUpTo(Screen.Artists.route) { inclusive = false }
                launchSingleTop = true
            }
        }

        Spacer(Modifier.weight(1f))

        HorizontalDivider(
            color = colors.border,
            thickness = 0.5.dp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(8.dp))

        SidebarItem(
            label = "Settings",
            icon = Icons.Default.Settings,
            selected = currentRoute == Screen.Settings.route,
            colors = colors,
        ) {
            navController.navigate(Screen.Settings.route) {
                popUpTo(Screen.Artists.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }
}

@Composable
private fun SidebarSectionLabel(text: String, colors: PulseColors) {
    Text(
        text = text,
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = colors.textDim,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
    )
}

@Composable
private fun SidebarItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    colors: PulseColors,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .then(
                if (selected) Modifier.background(colors.greenDim, RoundedCornerShape(6.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) colors.green else colors.textMuted,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) colors.green else colors.textMuted,
        )
    }
}
