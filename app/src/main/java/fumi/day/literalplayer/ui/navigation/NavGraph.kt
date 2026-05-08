package fumi.day.literalplayer.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import fumi.day.literalplayer.ui.artist.ArtistDetailScreen
import fumi.day.literalplayer.ui.list.TrackListScreen
import fumi.day.literalplayer.ui.player.PlayerScreen
import fumi.day.literalplayer.ui.player.PlayerViewModel
import fumi.day.literalplayer.ui.settings.SettingsScreen
import fumi.day.literalplayer.ui.setup.SetupScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val SETUP = "setup"
    const val LIST = "list"
    const val ARTIST_DETAIL = "artist/{artistName}"
    const val PLAYER = "player/{trackId}"
    const val SETTINGS = "settings"

    fun artistDetail(artistName: String) =
        "artist/${URLEncoder.encode(artistName, "UTF-8")}"

    fun player(trackId: String) =
        "player/${URLEncoder.encode(trackId, "UTF-8")}"
}

@Composable
fun NavGraph(
    navController: NavHostController,
    startDestination: String,
    playerViewModel: PlayerViewModel,
) {
    val playerState by playerViewModel.state.collectAsState()
    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.SETUP) {
            SetupScreen(onComplete = {
                navController.navigate(Routes.LIST) {
                    popUpTo(Routes.SETUP) { inclusive = true }
                }
            })
        }
        composable(Routes.LIST) {
            TrackListScreen(
                onTrackClick = { track ->
                    navController.navigate(Routes.player(track.id))
                },
                onArtistClick = { artistName ->
                    navController.navigate(Routes.artistDetail(artistName))
                },
                onAlbumPlay = { tracks ->
                    navController.navigate(Routes.player(tracks.first().id))
                },
                onNavigateToSettings = { navController.navigate(Routes.SETTINGS) },
                onNavigateToPlayer = {
                    playerViewModel.state.value.track?.id?.let {
                        navController.navigate(Routes.player(it))
                    }
                },
                playerViewModel = playerViewModel,
            )
        }
        composable(
            route = Routes.ARTIST_DETAIL,
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStack ->
            val artistName = URLDecoder.decode(backStack.arguments?.getString("artistName") ?: "", "UTF-8")
            ArtistDetailScreen(
                artistName = artistName,
                currentTrackId = playerState.track?.id,
                onTrackClick = { track -> navController.navigate(Routes.player(track.id)) },
                onAlbumPlay = { tracks -> navController.navigate(Routes.player(tracks.first().id)) },
                onBack = { navController.popBackStack() },
            )
        }
        composable(
            route = Routes.PLAYER,
            arguments = listOf(navArgument("trackId") { type = NavType.StringType })
        ) { backStack ->
            val trackId = URLDecoder.decode(backStack.arguments?.getString("trackId") ?: "", "UTF-8")
            PlayerScreen(
                trackId = trackId,
                onBack = { navController.popBackStack() },
                viewModel = playerViewModel,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
