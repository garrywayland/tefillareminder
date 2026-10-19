package com.garry.reminder.phone

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garry.reminder.Item

/** Full-screen reminder: stays on, shows over the lock screen, silences notifications until Done. */
class ReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val st = computeDue(this, record = true)
        if (Item.SHEMA in st.items) { // opening the reminder acknowledges Shema: clear its notification and let the widget revert to the Kotel
            getSystemService(android.app.NotificationManager::class.java).cancel(ShemaAlarms.NOTIFICATION_ID)
            ShemaAlarms.refreshWidget(this)
        }
        val wantsDnd = getSharedPreferences("p", 0).getBoolean("dnd", true)
        val silenced = wantsDnd && DndGuard.enable(this)
        setContent { ReminderScreen(st, silenced, wantsDnd && !silenced) { DndGuard.restore(this); finishAndRemoveTask() } }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) DndGuard.restore(this)
    }
}

@androidx.compose.runtime.Composable
fun ReminderScreen(st: DueState, silenced: Boolean, needsAccess: Boolean, onDone: () -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (st.purim) Image(painterResource(R.drawable.ic_panda), null, Modifier.fillMaxSize().padding(48.dp), alpha = 0.22f)
        Column(Modifier.fillMaxSize().padding(24.dp).systemBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(st.service.label + if (st.purim) " \u00b7 Purim" else "", color = Color.White, fontSize = 28.sp)
            Text(if (silenced) "Notifications silenced" else if (needsAccess) "Notifications NOT silenced \u2013 allow access in the app" else "",
                color = Color(0xFFA9A9B3), fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
            if (st.items.isEmpty()) {
                Spacer(Modifier.weight(1f))
                Image(painterResource(R.drawable.ic_kotel), null, Modifier.size(140.dp))
                Text("Nothing special today", color = Color.White, fontSize = 20.sp, modifier = Modifier.padding(top = 12.dp))
                Spacer(Modifier.weight(1f))
            } else {
                LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
                    items(st.items.size) { i ->
                        Column(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(st.items[i].heb, color = Color(0xFFE2B84A), fontSize = 46.sp, textAlign = TextAlign.Center)
                            Text(st.items[i].en, color = Color(0xFFA9A9B3), fontSize = 13.sp)
                        }
                    }
                }
            }
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth().height(64.dp)) { Text("Done", fontSize = 20.sp) }
        }
    }
}
