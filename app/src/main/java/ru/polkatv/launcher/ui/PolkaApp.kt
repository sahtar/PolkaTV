package ru.polkatv.launcher.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.ui.apps.AllAppsScreen
import ru.polkatv.launcher.ui.apps.AllAppsViewModel
import ru.polkatv.launcher.ui.home.HeaderActions
import ru.polkatv.launcher.ui.home.HomeScreen
import ru.polkatv.launcher.ui.home.HomeViewModel
import ru.polkatv.launcher.ui.theme.GlassFill

object Routes {
    const val HOME = "home"
    const val ALL_APPS = "allapps"
    const val WEATHER = "weather/{index}"
    const val SETTINGS = "settings"
    fun weather(index: Int) = "weather/$index"
}

@Composable
fun PolkaApp(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            val vm: HomeViewModel = viewModel(factory = viewModelFactory { initializer { HomeViewModel(container) } })
            HomeScreen(
                vm = vm,
                container = container,
                headerActions = HeaderActions({}, {}, {}, {}, {}),   // wired in Task 9/10
                onOpenAllApps = { nav.navigate(Routes.ALL_APPS) },  // screen arrives in Task 6
                weatherShelf = { m -> Box(m.background(GlassFill, RoundedCornerShape(24.dp))) },   // Task 8 replaces
            )
        }
        composable(Routes.ALL_APPS) {
            val vm: AllAppsViewModel = viewModel(factory = viewModelFactory { initializer { AllAppsViewModel(container) } })
            AllAppsScreen(vm = vm, container = container, onBack = { nav.popBackStack() })
        }
    }
}
