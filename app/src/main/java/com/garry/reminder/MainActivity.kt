package com.garry.reminder

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import java.time.LocalDate
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // stays lit while open
        setContent { App(onDone = { Ongoing.stop(this); finishAndRemoveTask() }) }
    }
}

@Composable
fun App(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("p", 0) }
    val today = remember { LocalDate.now().toEpochDay() }
    var service by remember { mutableStateOf(Service.SHACHARIT) }
    var auto by remember { mutableStateOf(true) }
    var israel by remember { mutableStateOf(false) }
    var tick by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<String?>(null) }
    val manual = remember { mutableStateListOf<Item>() }

    // Has Mashiv/Morid or Tal/Bracha switched since the last time we asked?
    fun check() {
        val (m, t) = longTermState(Calendar.getInstance(), israel)
        val e = prefs.edit()
        if (prefs.getInt("m", -1) == -1) e.putInt("m", m)
        if (prefs.getInt("t", -1) == -1) e.putInt("t", t)
        e.apply()
        pending = when {
            prefs.getInt("m", -1) != m -> "m"
            prefs.getInt("t", -1) != t -> "t"
            else -> null
        }
    }
    fun answer(key: String, days: Int) {
        val (m, t) = longTermState(Calendar.getInstance(), israel)
        prefs.edit().putInt(key, if (key == "m") m else t).putLong(key + "End", today + days - 1).apply()
        tick++; check()
    }
    LaunchedEffect(Unit) { check() }

    val mOn = tick >= 0 && prefs.getLong("mEnd", -1) >= today
    val tOn = tick >= 0 && prefs.getLong("tEnd", -1) >= today
    val items = if (auto) autoItems(Calendar.getInstance(), service, israel, mOn, tOn)
                else Item.values().filter { it in manual }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { perm.launch(Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(items, service) {
        Ongoing.start(ctx, service.label + ": " + items.joinToString(", ") { it.en.substringBefore(" /") }.take(60))
    }

    MaterialTheme {
        Scaffold(timeText = { TimeText() }) {
            ScalingLazyColumn(Modifier) {
                val p = pending
                if (p != null) {
                    val (m, t) = longTermState(Calendar.getInstance(), israel)
                    val msg = if (p == "m") (if (m == 1) "Mashiv HaRuach" else "Morid HaTal") else (if (t == 1) "V'ten Tal U'matar" else "V'ten Bracha")
                    item { Text("$msg starts today.\nRemind me for how long?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    items(listOf(5, 10, 30)) { d -> Chip(label = { Text("$d days") }, onClick = { answer(p, d) }) }
                    item { Chip(label = { Text("No reminder") }, onClick = { answer(p, 0) }, colors = ChipDefaults.secondaryChipColors()) }
                } else {
                    item { Chip(label = { Text(service.label) }, onClick = { service = Service.values()[(service.ordinal + 1) % 3] }) }
                    item { ToggleChip(checked = auto, onCheckedChange = { auto = it }, label = { Text("Auto (calendar)") },
                        toggleControl = { Switch(checked = auto, onCheckedChange = null) }) }
                    item { ToggleChip(checked = israel, onCheckedChange = { israel = it }, label = { Text("Israel") },
                        toggleControl = { Switch(checked = israel, onCheckedChange = null) }) }
                    if (auto) {
                        items(items.size) { i ->
                            Card(onClick = {}) {
                                Text(items[i].heb, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                Text(items[i].en, style = MaterialTheme.typography.caption2)
                            }
                        }
                    } else {
                        items(Item.values().size) { i ->
                            val it = Item.values()[i]
                            ToggleChip(checked = it in manual, onCheckedChange = { on -> if (on) manual.add(it) else manual.remove(it) },
                                label = { Text(it.heb) }, secondaryLabel = { Text(it.en) },
                                toggleControl = { Checkbox(checked = it in manual, onCheckedChange = null) })
                        }
                    }
                    item { Chip(label = { Text("Done") }, onClick = onDone, colors = ChipDefaults.secondaryChipColors()) }
                }
            }
        }
    }
}
