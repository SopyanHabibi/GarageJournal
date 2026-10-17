package com.ian.garagejournal.ui.navigation

object Routes {
    const val VEHICLE_LIST = "vehicles"
    const val VEHICLE_FORM = "vehicle_form?vehicleId={vehicleId}"
    const val VEHICLE_DETAIL = "vehicle_detail/{vehicleId}"

    fun vehicleForm(id: Long? = null) =
        if (id == null) "vehicle_form" else "vehicle_form?vehicleId=$id"

    fun vehicleDetail(id: Long) = "vehicle_detail/$id"
}