package com.ian.garagejournal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.ian.garagejournal.notification.ReminderScheduler
import com.ian.garagejournal.ui.navigation.GarageNavHost
import com.ian.garagejournal.ui.theme.GarageJournalTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ReminderScheduler.checkNow(applicationContext)
        enableEdgeToEdge()
        val app = application as GarageApp
        setContent {
            GarageJournalTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    GarageNavHost(app.repository, app.receiptStorage)
                }
            }
        }
    }
}