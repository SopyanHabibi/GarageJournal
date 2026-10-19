package com.ian.garagejournal.ui.navigation

object Routes {
    const val VEHICLE_LIST = "vehicles"
    const val VEHICLE_FORM = "vehicle_form?vehicleId={vehicleId}"
    const val VEHICLE_DETAIL = "vehicle_detail/{vehicleId}"
    const val SERVICE_LIST = "service_list/{vehicleId}"
    const val SERVICE_FORM = "service_form/{vehicleId}?logId={logId}"
    const val FUEL_LIST = "fuel_list/{vehicleId}"
    const val FUEL_FORM = "fuel_form/{vehicleId}?logId={logId}"
    const val REMINDER_FORM = "reminder_form/{vehicleId}?reminderId={reminderId}"


    fun vehicleForm(id: Long? = null) =
        if (id == null) "vehicle_form" else "vehicle_form?vehicleId=$id"
    fun vehicleDetail(id: Long) = "vehicle_detail/$id"
    fun serviceList(vehicleId: Long) = "service_list/$vehicleId"
    fun serviceForm(vehicleId: Long, logId: Long? = null) =
        if (logId == null) "service_form/$vehicleId" else "service_form/$vehicleId?logId=$logId"

    fun fuelList(vehicleId: Long) = "fuel_list/$vehicleId"
    fun fuelForm(vehicleId: Long, logId: Long? = null) =
        if (logId == null) "fuel_form/$vehicleId" else "fuel_form/$vehicleId?logId=$logId"
    fun reminderForm(vehicleId: Long, reminderId: Long? = null) =
        if (reminderId == null) "reminder_form/$vehicleId"
        else "reminder_form/$vehicleId?reminderId=$reminderId"
}