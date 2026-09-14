package com.bbobbo.pet.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.bbobbo.pet.ui.actions.BathScreen
import com.bbobbo.pet.ui.actions.PlayScreen
import com.bbobbo.pet.ui.actions.SleepScreen
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.home.HomeScreen
import com.bbobbo.pet.ui.minigame.MiniGameScreen
import com.bbobbo.pet.ui.room.RoomDecorScreen
import com.bbobbo.pet.ui.wardrobe.WardrobeScreen

object Routes {
    const val HOME = "home"
    const val BATH = "bath"
    const val SLEEP = "sleep"
    const val PLAY = "play"
    const val GAME = "game"
    const val WARDROBE = "wardrobe"
    const val ROOM = "room"
}

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    val vm: PetViewModel = viewModel()

    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(vm, nav) }
        composable(Routes.BATH) { BathScreen(vm) { nav.popBackStack() } }
        composable(Routes.SLEEP) { SleepScreen(vm) { nav.popBackStack() } }
        composable(Routes.PLAY) { PlayScreen(vm) { nav.popBackStack() } }
        composable(Routes.GAME) { MiniGameScreen(vm) { nav.popBackStack() } }
        composable(Routes.WARDROBE) { WardrobeScreen(vm) { nav.popBackStack() } }
        composable(Routes.ROOM) { RoomDecorScreen(vm) { nav.popBackStack() } }
    }
}
