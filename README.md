# Garage Journal - Motorcycle Maintenance & Fuel Log

**Garage Journal** adalah aplikasi Android Native (*offline-first*) yang dirancang untuk membantu pemilik kendaraan/motor mencatat riwayat perawatan (*maintenance*) serta memantau tingkat efisiensi bahan bakar secara akurat.

Project ini dibangun dengan fokus menerapkan *modern Android development practices* (Jetpack Compose, Clean Architecture, dan Room Database).

---

## Fitur Utama

- **Manajemen Kendaraan (Multi-Vehicle):** Kelola profil beberapa kendaraan sekaligus beserta data odometer awal dan terbaru.
- **Log Perawatan & Servis:** Catat riwayat pergantian part, ganti oli, hingga biaya perawatan secara mendetail.
- **Kalkulator & Log BBM:** Hitung otomatis tingkat konsumsi bahan bakar (**KM/Liter**) setiap kali melakukan pengisian bensin.
- **Offline-First Data Storage:** Seluruh data tersimpan aman secara lokal di perangkat pengguna.

---

## Tech Stack & Architecture

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** Jetpack Compose
- **Architecture:** MVVM (Model-View-ViewModel) + Clean Architecture
- **Database:** Room Database (SQLite abstraction)
- **Asynchronous & Flow:** Kotlin Coroutines & StateFlow
- **Navigation:** Jetpack Compose Navigation

---

## Schema Database (ERD)

```text
+-----------------------+       1 : N       +-----------------------+
|        VEHICLE        |-------------------|    MAINTENANCE_LOG    |
+-----------------------+                   +-----------------------+
| PK | id               |                   | PK | id               |
|    | name             |                   | FK | vehicle_id       |
|    | license_plate    |                   |    | service_type     |
|    | current_odometer |                   |    | cost             |
+-----------------------+                   |    | date_millis      |
            |                               +-----------------------+
            | 1 : N
            |                               +-----------------------+
            +-------------------------------|       FUEL_LOG        |
                                            +-----------------------+
                                            | PK | id               |
                                            | FK | vehicle_id       |
                                            |    | fuel_liters      |
                                            |    | total_cost       |
                                            |    | odometer         |
                                            +-----------------------+
