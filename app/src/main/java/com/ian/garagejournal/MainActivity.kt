package com.ian.garagejournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.ian.garagejournal.ui.navigation.GarageNavHost
import com.ian.garagejournal.ui.theme.GarageJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as GarageApp
        setContent {
            GarageJournalTheme {
                GarageNavHost(app.repository, app.receiptStorage)
            }
        }
    }
}