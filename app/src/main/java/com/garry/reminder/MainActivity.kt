package com.garry.reminder

import android.Manifest
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.material.*
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.util.Calendar

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // stays lit while open
        setContent { App(onDone = { Ongoing.stop(this); finishAndRemoveTask() }) }
    }
}

private fun todayEpoch() = LocalDate.now().toEpochDay()

@Composable
fun App(onDone: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("p", 0) }
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var svcMode by remember { mutableStateOf<Service?>(null) } // null = automatic by time of day
    var auto by remember { mutableStateOf(true) }
    var israel by remember { mutableStateOf(prefs.getBoolean("il", false)) }
    var tick by remember { mutableIntStateOf(0) }
    var pending by remember { mutableStateOf<String?>(null) }
    val manual = remember { mutableStateListOf<Item>() }

    LaunchedEffect(Unit) { while (true) { delay(60_000); nowMs = System.currentTimeMillis() } }

    fun check() {
        val (m, t) = longTermState(Calendar.getInstance(), israel)
        val e = prefs.edit()
        if (prefs.getInt("m", -1) == -1) e.putInt("m", m)
        if (prefs.getInt("t", -1) == -1) e.putInt("t", t)
        if (m == 0) e.putInt("m", 0) // summer: nothing to remind, just record it
        e.apply()
        pending = when {
            prefs.getInt("m", -1) != m -> "m"
            prefs.getInt("t", -1) != t -> "t"
            else -> null
        }
    }
    fun answer(key: String, days: Int) {
        val (m, t) = longTermState(Calendar.getInstance(), israel)
        prefs.edit().putInt(key, if (key == "m") m else t).putLong(key + "End", todayEpoch() + days - 1).apply()
        tick++; check()
    }
    LaunchedEffect(Unit) { check() }

    val now = Calendar.getInstance().apply { timeInMillis = nowMs }
    val service = svcMode ?: autoService(nowMs)
    val mOn = tick >= 0 && prefs.getLong("mEnd", -1) >= todayEpoch()
    val tOn = tick >= 0 && prefs.getLong("tEnd", -1) >= todayEpoch()
    val due = if (auto) autoItems(now, service, israel, mOn, tOn) else Item.values().filter { it in manual }

    val purim = isPurim(now, service, israel)

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) { perm.launch(Manifest.permission.POST_NOTIFICATIONS) }
    LaunchedEffect(due, service) {
        Ongoing.start(ctx, service.label + ": " + due.joinToString(", ") { it.en.substringBefore(" /") }.take(60))
    }

    MaterialTheme {
        Scaffold(timeText = { TimeText() }) {
            Box(Modifier.fillMaxSize()) {
            if (purim && pending == null) {
                Image(painterResource(R.drawable.ic_panda), contentDescription = null, alpha = 0.3f, modifier = Modifier.fillMaxSize().padding(28.dp))
            }
            ScalingLazyColumn(Modifier) {
                val p = pending
                if (p != null) {
                    val (m, t) = longTermState(Calendar.getInstance(), israel)
                    val msg = if (p == "m") "Morid HaGeshem" else (if (t == 1) "V'ten Tal U'matar" else "V'ten Bracha")
                    item { Text("$msg starts today.\nRemind me for how long?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    items(3) { i ->
                        val d = listOf(5, 10, 30)[i]
                        Chip(label = { Text("$d days") }, onClick = { answer(p, d) })
                    }
                    item { Chip(label = { Text("No reminder") }, onClick = { answer(p, 0) }, colors = ChipDefaults.secondaryChipColors()) }
                } else {
                    // 1. The reminder itself
                    item { Text(service.label, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.title3) }
                    if (due.isEmpty()) {
                        item { Text("Nothing special today", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                    }
                    items(due.size) { i ->
                        Card(onClick = {}) {
                            Text(due[i].heb, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            Text(due[i].en, style = MaterialTheme.typography.caption2)
                        }
                    }
                    item { Chip(label = { Text("Done") }, onClick = onDone, colors = ChipDefaults.secondaryChipColors()) }

                    // 2. Menu (scroll down)
                    item { Text("— Menu —", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.caption1) }
                    item {
                        Chip(
                            label = { Text("Service") },
                            secondaryLabel = { Text(if (svcMode == null) "Auto (${service.label})" else service.label) },
                            onClick = {
                                svcMode = when (svcMode) {
                                    null -> Service.SHACHARIT
                                    Service.SHACHARIT -> Service.MINCHA
                                    Service.MINCHA -> Service.MAARIV
                                    Service.MAARIV -> null
                                }
                            }
                        )
                    }
                    item { Chip(label = { Text("Reminders") }, secondaryLabel = { Text(if (auto) "Automatic (calendar)" else "Manual") }, onClick = { auto = !auto }) }
                    item { Chip(label = { Text("Location") }, secondaryLabel = { Text(if (israel) "Israel" else "Diaspora") }, onClick = { israel = !israel; prefs.edit().putBoolean("il", israel).apply(); check() }) }
                    if (!auto) {
                        items(Item.values().size) { i ->
                            val x = Item.values()[i]
                            Chip(label = { Text((if (x in manual) "✓ " else "") + x.heb) }, secondaryLabel = { Text(x.en) },
                                onClick = { if (x in manual) manual.remove(x) else manual.add(x) })
                        }
                    }
                    item { Chip(label = { Text("Allow notifications") }, onClick = { perm.launch(Manifest.permission.POST_NOTIFICATIONS) }, colors = ChipDefaults.secondaryChipColors()) }
                    item {
                        Text(
                            "Why notifications? They let this app keep a small siddur chip on your watch face while you daven, so the reminder stays visible until you tap Done. No location or personal data is used or sent anywhere. Prayer times (alos, chatzot, shkia) are worked out on the watch for Edgware, London.",
                            style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    item { Image(painterResource(R.drawable.ic_panda), contentDescription = null, modifier = Modifier.size(32.dp)) }
                    item { Text("Great Panda Workflows", style = MaterialTheme.typography.caption2, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) }
                }
            }
            }
        }
    }
}
