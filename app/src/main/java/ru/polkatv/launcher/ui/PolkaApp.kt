package ru.polkatv.launcher.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ru.polkatv.launcher.AppContainer
import ru.polkatv.launcher.ui.apps.AllAppsScreen
import ru.polkatv.launcher.ui.apps.AllAppsViewModel
import ru.polkatv.launcher.ui.home.HomeScreen
import ru.polkatv.launcher.ui.home.HomeViewModel
import ru.polkatv.launcher.ui.settings.SettingsScreen
import ru.polkatv.launcher.ui.settings.SettingsViewModel
import ru.polkatv.launcher.ui.weather.WeatherDetailScreen
import ru.polkatv.launcher.weather.CityWeather

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
                onOpenAllApps = { nav.navigate(Routes.ALL_APPS) },
                onOpenWeather = { nav.navigate(Routes.weather(it)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(Routes.SETTINGS) {
            val vm: SettingsViewModel = viewModel(factory = viewModelFactory { initializer { SettingsViewModel(container) } })
            SettingsScreen(vm = vm, onBack = { nav.popBackStack() })
        }
        composable(Routes.ALL_APPS) {
            val vm: AllAppsViewModel = viewModel(factory = viewModelFactory { initializer { AllAppsViewModel(container) } })
            AllAppsScreen(vm = vm, container = container, onBack = { nav.popBackStack() })
        }
        composable(Routes.WEATHER, arguments = listOf(navArgument("index") { type = NavType.IntType })) { entry ->
            val index = entry.arguments?.getInt("index") ?: 0
            val prefs by container.prefs.collectAsStateWithLifecycle()
            val states by container.weather.states.collectAsStateWithLifecycle()
            val city = prefs.cities.getOrNull(index)
            WeatherDetailScreen(cityWeather = city?.let { states[it.key] ?: CityWeather(it) }, onBack = { nav.popBackStack() })
        }
    }
}
