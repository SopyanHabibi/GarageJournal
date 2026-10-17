package com.ian.garagejournal.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ian.garagejournal.ui.navigation.Routes
import com.ian.garagejournal.data.repository.GarageRepository
import com.ian.garagejournal.ui.vehicle.*

@Composable
fun GarageNavHost(repository: GarageRepository) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.VEHICLE_LIST) {

        composable(Routes.VEHICLE_LIST) {
            VehicleListScreen(
                viewModel = viewModel(factory = VehicleListViewModel.factory(repository)),
                onAdd = { navController.navigate(Routes.vehicleForm()) },
                onOpen = { navController.navigate(Routes.vehicleDetail(it)) },
                onEdit = { navController.navigate(Routes.vehicleForm(it)) }
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
            route = Routes.VEHICLE_DETAIL,
            arguments = listOf(navArgument("vehicleId") { type = NavType.LongType })
        ) { entry ->
            val id = entry.arguments?.getLong("vehicleId") ?: return@composable
            VehicleDetailScreen(
                viewModel = viewModel(factory = VehicleDetailViewModel.factory(repository, id)),
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.vehicleForm(id)) }
            )
        }
    }
}