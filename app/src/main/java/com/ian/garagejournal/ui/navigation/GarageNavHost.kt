package com.ian.garagejournal.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ian.garagejournal.data.local.ReceiptStorage
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.ui.backup.BackupViewModel
import com.ian.garagejournal.ui.fuel.FuelFormScreen
import com.ian.garagejournal.ui.fuel.FuelFormViewModel
import com.ian.garagejournal.ui.fuel.FuelListScreen
import com.ian.garagejournal.ui.fuel.FuelListViewModel
import com.ian.garagejournal.ui.reminder.ReminderFormScreen
import com.ian.garagejournal.ui.reminder.ReminderFormViewModel
import com.ian.garagejournal.ui.service.ServiceFormScreen
import com.ian.garagejournal.ui.service.ServiceFormViewModel
import com.ian.garagejournal.ui.service.ServiceListScreen
import com.ian.garagejournal.ui.service.ServiceListViewModel
import com.ian.garagejournal.ui.stats.StatsScreen
import com.ian.garagejournal.ui.stats.StatsViewModel
import com.ian.garagejournal.ui.vehicle.VehicleDetailViewModel
import com.ian.garagejournal.ui.vehicle.VehicleFormScreen
import com.ian.garagejournal.ui.vehicle.VehicleFormViewModel
import com.ian.garagejournal.ui.vehicle.VehicleListScreen
import com.ian.garagejournal.ui.vehicle.VehicleListViewModel


@Composable
fun GarageNavHost(repository: GarageRepository, receiptStorage: ReceiptStorage) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.VEHICLE_LIST,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { it / 3 },
                animationSpec = tween(280)
            ) + fadeIn(animationSpec = tween(280))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { -it / 3 },
                animationSpec = tween(280)
            ) + fadeOut(animationSpec = tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { -it / 3 },
                animationSpec = tween(280)
            ) + fadeIn(animationSpec = tween(280))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { it / 3 },
                animationSpec = tween(280)
            ) + fadeOut(animationSpec = tween(200))
        }
    ) {

        composable(Routes.VEHICLE_LIST) {
            VehicleListScreen(
                viewModel = viewModel(factory = VehicleListViewModel.factory(repository)),
                backupViewModel = viewModel(factory = BackupViewModel.Factory),
                repository = repository,
                onAdd = { navController.navigate(Routes.vehicleForm()) },
                onEditVehicle = { navController.navigate(Routes.vehicleForm(it)) },
                onOpenServiceLog = { navController.navigate(Routes.serviceList(it)) },
                onOpenFuelLog = { navController.navigate(Routes.fuelList(it)) },
                onAddReminder = { navController.navigate(Routes.reminderForm(it)) },
                onEditReminder = { vehicleId, reminderId ->
                    navController.navigate(Routes.reminderForm(vehicleId, reminderId))
                },
                onOpenStats = { navController.navigate(Routes.stats(it)) },
            )
        }

        composable(
            route = Routes.VEHICLE_FORM,
            arguments = listOf(navArgument("vehicleId") {
                type = NavType.LongType
                defaultValue = -1L
            })
        ) { entry ->
            val id = entry.arguments?.getLong("vehicleId") ?: -1L
            VehicleFormScreen(
                viewModel = viewModel(factory = VehicleFormViewModel.factory(repository, id)),
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.SERVICE_LIST,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { entry ->
            val vehicleId = entry.arguments?.getLong("vehicleId") ?: return@composable
            ServiceListScreen(
                viewModel = viewModel(
                    factory = ServiceListViewModel.factory(repository, receiptStorage, vehicleId)
                ),
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.serviceForm(vehicleId)) },
                onEdit = { logId -> navController.navigate(Routes.serviceForm(vehicleId, logId)) }
            )
        }

        composable(
            route = Routes.SERVICE_FORM,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("logId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getLong("vehicleId") ?: return@composable
            val logId = entry.arguments?.getLong("logId") ?: -1L
            ServiceFormScreen(
                viewModel = viewModel(
                    factory = ServiceFormViewModel.factory(repository, receiptStorage, vehicleId, logId)
                ),
                onDone = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.FUEL_LIST,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { entry ->
            val vehicleId = entry.arguments?.getLong("vehicleId") ?: return@composable
            FuelListScreen(
                viewModel = viewModel(factory = FuelListViewModel.factory(repository, vehicleId)),
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate(Routes.fuelForm(vehicleId)) },
                onEdit = { logId -> navController.navigate(Routes.fuelForm(vehicleId, logId)) }
            )
        }

        composable(
            route = Routes.FUEL_FORM,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("logId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getLong("vehicleId") ?: return@composable
            val logId = entry.arguments?.getLong("logId") ?: -1L
            FuelFormScreen(
                viewModel = viewModel(factory = FuelFormViewModel.factory(repository, vehicleId, logId)),
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.REMINDER_FORM,
            arguments = listOf(
                navArgument("vehicleId") { type = NavType.LongType },
                navArgument("reminderId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { entry ->
            val vehicleId = entry.arguments?.getLong("vehicleId") ?: return@composable
            val reminderId = entry.arguments?.getLong("reminderId") ?: -1L
            ReminderFormScreen(
                viewModel = viewModel(
                    factory = ReminderFormViewModel.factory(repository, vehicleId, reminderId)
                ),
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = Routes.STATS,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { entry ->
            val vehicleId = entry.arguments?.getLong("vehicleId") ?: return@composable
            StatsScreen(
                viewModel = viewModel(factory = StatsViewModel.factory(repository, vehicleId)),
                onBack = { navController.popBackStack() }
            )
        }
    }
}