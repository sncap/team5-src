package com.bbobbo.pet.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bbobbo.pet.ui.actions.BathScreen
import com.bbobbo.pet.ui.actions.PlayScreen
import com.bbobbo.pet.ui.actions.SleepScreen
import com.bbobbo.pet.ui.attendance.AttendanceScreen
import com.bbobbo.pet.ui.collection.CollectionScreen
import com.bbobbo.pet.ui.common.PetViewModel
import com.bbobbo.pet.ui.components.BottomTab
import com.bbobbo.pet.ui.components.BottomTabBar
import com.bbobbo.pet.ui.home.HomeScreen
import com.bbobbo.pet.ui.inventory.InventoryScreen
import com.bbobbo.pet.ui.minigame.MiniGameScreen
import com.bbobbo.pet.ui.more.MoreScreen
import com.bbobbo.pet.ui.onboarding.SplashGate
import com.bbobbo.pet.ui.pet.PetDetailScreen
import com.bbobbo.pet.ui.room.RoomDecorScreen
import com.bbobbo.pet.ui.settings.SettingsScreen
import com.bbobbo.pet.ui.shop.ShopScreen
import com.bbobbo.pet.ui.walk.WalkScreen
import com.bbobbo.pet.ui.wardrobe.WardrobeScreen

object Routes {
    // 하단 탭
    const val HOME = "home"
    const val PET = "pet"
    const val INVENTORY = "inventory"
    const val SHOP = "shop"
    const val MORE = "more"

    // 전체 화면
    const val BATH = "bath"
    const val SLEEP = "sleep"
    const val PLAY = "play"
    const val GAME = "game"
    const val WARDROBE = "wardrobe"
    const val ROOM = "room"
    const val WALK = "walk"
    const val COLLECTION = "collection"
    const val ATTENDANCE = "attendance"
    const val SETTINGS = "settings"
}

private val BOTTOM_TABS = listOf(
    BottomTab(Routes.HOME, "홈", "🏠"),
    BottomTab(Routes.PET, "뽀뽀", "🐶"),
    BottomTab(Routes.INVENTORY, "보관함", "🎒"),
    BottomTab(Routes.SHOP, "상점", "🛒"),
    BottomTab(Routes.MORE, "더보기", "⋯"),
)

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    val vm: PetViewModel = viewModel()

    // 스플래시/온보딩을 통과해야 본 화면이 보인다 (기획서 S-00).
    SplashGate(vm) {
        val backStack by nav.currentBackStackEntryAsState()
        val route = backStack?.destination?.route
        val showTabs = remember(route) { BOTTOM_TABS.any { it.route == route } }

        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f)) {
                NavHost(navController = nav, startDestination = Routes.HOME) {
                    tabDestinations(vm, nav)
                    fullScreenDestinations(vm, nav)
                }
            }
            if (showTabs) {
                BottomTabBar(
                    tabs = BOTTOM_TABS,
                    currentRoute = route,
                    onSelect = { nav.switchTab(it.route) },
                )
            }
        }
    }
}

private fun NavGraphBuilder.tabDestinations(vm: PetViewModel, nav: NavHostController) {
    composable(Routes.HOME) { HomeScreen(vm, nav) }
    composable(Routes.PET) {
        PetDetailScreen(vm, onOpenSettings = { nav.navigate(Routes.SETTINGS) })
    }
    composable(Routes.INVENTORY) {
        InventoryScreen(
            vm = vm,
            onOpenWardrobe = { nav.navigate(Routes.WARDROBE) },
            onOpenRoom = { nav.navigate(Routes.ROOM) },
            onBack = { nav.switchTab(Routes.HOME) },
        )
    }
    composable(Routes.SHOP) { ShopScreen(vm) { nav.switchTab(Routes.HOME) } }
    composable(Routes.MORE) {
        MoreScreen(
            vm = vm,
            onCollection = { nav.navigate(Routes.COLLECTION) },
            onAttendance = { nav.navigate(Routes.ATTENDANCE) },
            onSettings = { nav.navigate(Routes.SETTINGS) },
        )
    }
}

private fun NavGraphBuilder.fullScreenDestinations(vm: PetViewModel, nav: NavHostController) {
    composable(Routes.BATH) { BathScreen(vm) { nav.popBackStack() } }
    composable(Routes.SLEEP) { SleepScreen(vm) { nav.popBackStack() } }
    composable(Routes.PLAY) { PlayScreen(vm) { nav.popBackStack() } }
    composable(Routes.GAME) { MiniGameScreen(vm) { nav.popBackStack() } }
    composable(Routes.WARDROBE) { WardrobeScreen(vm) { nav.popBackStack() } }
    composable(Routes.ROOM) { RoomDecorScreen(vm) { nav.popBackStack() } }
    composable(Routes.WALK) { WalkScreen(vm) { nav.popBackStack() } }
    composable(Routes.COLLECTION) { CollectionScreen(vm) { nav.popBackStack() } }
    composable(Routes.ATTENDANCE) { AttendanceScreen(vm) { nav.popBackStack() } }
    composable(Routes.SETTINGS) { SettingsScreen(vm) { nav.popBackStack() } }
}

/**
 * 탭 전환은 백스택을 쌓지 않는다. 탭을 오가다 뒤로가기를 누르면
 * 지나온 탭을 되짚는 대신 홈으로 돌아오도록 시작 지점까지 popUp 한다.
 */
private fun NavHostController.switchTab(route: String) {
    if (currentDestination?.route == route) return
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
