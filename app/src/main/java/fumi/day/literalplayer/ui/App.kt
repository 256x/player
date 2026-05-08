package fumi.day.literalplayer.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import fumi.day.literalplayer.ui.navigation.NavGraph
import fumi.day.literalplayer.ui.navigation.Routes
import fumi.day.literalplayer.ui.player.PlayerViewModel
import fumi.day.literalplayer.ui.theme.LiteralPlayerTheme

@Composable
fun App(viewModel: AppViewModel = hiltViewModel()) {
    val prefs by viewModel.prefs.collectAsState()
    val navController = rememberNavController()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val startDestination = if (prefs.rootFolders.isEmpty()) Routes.SETUP else Routes.LIST

    LiteralPlayerTheme(userPrefs = prefs) {
        NavGraph(
            navController = navController,
            startDestination = startDestination,
            playerViewModel = playerViewModel,
        )
    }
}
