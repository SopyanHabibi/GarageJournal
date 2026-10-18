package com.ian.garagejournal.ui.navigation

object Routes {
    const val VEHICLE_LIST = "vehicles"
    const val VEHICLE_FORM = "vehicle_form?vehicleId={vehicleId}"
    const val VEHICLE_DETAIL = "vehicle_detail/{vehicleId}"
    const val SERVICE_LIST = "service_list/{vehicleId}"
    const val SERVICE_FORM = "service_form/{vehicleId}?logId={logId}"

    fun vehicleForm(id: Long? = null) =
        if (id == null) "vehicle_form" else "vehicle_form?vehicleId=$id"

    fun vehicleDetail(id: Long) = "vehicle_detail/$id"

    fun serviceList(vehicleId: Long) = "service_list/$vehicleId"
    fun serviceForm(vehicleId: Long, logId: Long? = null) =
        if (logId == null) "service_form/$vehicleId" else "service_form/$vehicleId?logId=$logId"
}