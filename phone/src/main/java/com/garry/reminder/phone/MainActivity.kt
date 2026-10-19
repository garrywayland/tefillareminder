package com.garry.reminder.phone

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
    private var locTick by mutableIntStateOf(0)
    private var ilNow by mutableStateOf(false)

    private fun refreshLocation() = Loc.refresh(this) { ilNow = getSharedPreferences("p", 0).getBoolean("il", false); locTick++ }
    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val locPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> if (ok) refreshLocation() }

    override fun onResume() {
        super.onResume()
        granted = getSystemService(NotificationManager::class.java).isNotificationPolicyAccessGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("p", 0)
        Loc.apply(this)
        ShemaAlarms.schedule(this)
        ilNow = prefs.getBoolean("il", false)
        if (Loc.hasPermission(this)) refreshLocation()
        else if (!prefs.getBoolean("locAsked", false)) { prefs.edit().putBoolean("locAsked", true).apply(); locPerm.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }
        setContent {
            var dnd by remember { mutableStateOf(prefs.getBoolean("dnd", true)) }
            var level by remember { mutableIntStateOf(prefs.getInt("dndLevel", 0)) }
            var minutes by remember { mutableIntStateOf(prefs.getInt("dndMinutes", 45)) }
            var shema by remember { mutableIntStateOf(prefs.getInt("shema", 0)) }
            var season by remember { mutableIntStateOf(prefs.getInt("seasonDays", 10)) }
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
                Button(onClick = {
                    shema = (shema + 1) % 3
                    prefs.edit().putInt("shema", shema).apply()
                    if (shema == 2 && android.os.Build.VERSION.SDK_INT >= 33) notifPerm.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    ShemaAlarms.schedule(this@MainActivity); ShemaAlarms.refreshWidget(this@MainActivity)
                }, Modifier.fillMaxWidth()) {
                    Text("Shema: " + listOf("off", "show on widget", "widget + notification")[shema])
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Israel", Modifier.weight(1f))
                    Switch(checked = ilNow, onCheckedChange = { ilNow = it; prefs.edit().putBoolean("il", it).apply() })
                }
                val ll = if (locTick >= 0) Loc.saved(this@MainActivity) else null
                Button(onClick = { if (Loc.hasPermission(this@MainActivity)) refreshLocation() else locPerm.launch(android.Manifest.permission.ACCESS_COARSE_LOCATION) }, Modifier.fillMaxWidth()) {
                    Text("Update location: " + if (ll == null) "Edgware (default)" else "%.2f, %.2f".format(ll.first, ll.second))
                }
                Text("Why Do Not Disturb access? Android only lets an app change silencing if you allow it here once. The app switches it on when the reminder opens and always puts your previous setting back when you tap Done (or after the time above). Nothing is sent anywhere. Your rough location is used only on this phone to work out prayer times and is never sent anywhere. Until a location is found it uses Edgware, London.", fontSize = 12.sp)
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(painterResource(R.drawable.ic_panda), null, Modifier.size(40.dp))
                    Text("Great Panda Workflows", fontSize = 12.sp)
                }
            }
        }
    }
}
