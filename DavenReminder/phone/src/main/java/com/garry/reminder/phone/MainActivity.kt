package com.garry.reminder.phone

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Settings screen (launcher). The reminder itself lives in ReminderActivity / the widget. */
class MainActivity : ComponentActivity() {
    private var granted by mutableStateOf(false)

    override fun onResume() {
        super.onResume()
        granted = getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("p", 0)
        setContent {
            var dnd by remember { mutableStateOf(prefs.getBoolean("dnd", true)) }
            var level by remember { mutableIntStateOf(prefs.getInt("dndLevel", 0)) }
            var minutes by remember { mutableIntStateOf(prefs.getInt("dndMinutes", 45)) }
            var season by remember { mutableIntStateOf(prefs.getInt("seasonDays", 10)) }
            var il by remember { mutableStateOf(prefs.getBoolean("il", false)) }
            Column(Modifier.fillMaxSize().systemBarsPadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Daven Reminder", fontSize = 24.sp)
                Text("Add the widget from your home screen's widget list. Tap it to open the full-screen reminder.", fontSize = 14.sp)
                Button(onClick = { startActivity(Intent(this@MainActivity, ReminderActivity::class.java)) }, Modifier.fillMaxWidth()) { Text("Preview reminder now") }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Silence notifications while open", Modifier.weight(1f))
                    Switch(checked = dnd, onCheckedChange = { dnd = it; prefs.edit().putBoolean("dnd", it).apply() })
                }
                if (!granted) Button(onClick = { startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)) }, Modifier.fillMaxWidth()) { Text("Allow Do Not Disturb access") }
                else Text("Do Not Disturb access: allowed", fontSize = 13.sp)
                Button(onClick = { level = (level + 1) % 3; prefs.edit().putInt("dndLevel", level).apply() }, Modifier.fillMaxWidth()) {
                    Text("Silence level: " + listOf("Alarms only", "Priority only", "Total silence")[level])
                }
                Button(onClick = { minutes = when (minutes) { 30 -> 45; 45 -> 60; else -> 30 }; prefs.edit().putInt("dndMinutes", minutes).apply() }, Modifier.fillMaxWidth()) {
                    Text("Auto-restore after: $minutes min")
                }
                Button(onClick = { season = when (season) { 5 -> 10; 10 -> 30; 30 -> 0; else -> 5 }; prefs.edit().putInt("seasonDays", season).apply() }, Modifier.fillMaxWidth()) {
                    Text("Season reminders (Morid HaGeshem, V'ten Tal/Bracha): " + if (season == 0) "off" else "$season days")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Israel", Modifier.weight(1f))
                    Switch(checked = il, onCheckedChange = { il = it; prefs.edit().putBoolean("il", it).remove("m").remove("t").remove("mStart").remove("tStart").apply() })
                }
                Text("Why Do Not Disturb access? Android only lets an app change silencing if you allow it here once. The app switches it on when the reminder opens and always puts your previous setting back when you tap Done (or after the time above). Nothing is sent anywhere. Prayer times are worked out on the phone for Edgware, London.", fontSize = 12.sp)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_panda), null, Modifier.size(40.dp))
                    Text("Great Panda Workflows", fontSize = 12.sp)
                }
            }
        }
    }
}
