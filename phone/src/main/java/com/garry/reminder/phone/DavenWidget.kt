package com.garry.reminder.phone

import android.content.Context
import android.content.Intent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider

class DavenWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val st = computeDue(context)
        provideContent {
            Column(
                modifier = GlanceModifier.fillMaxSize().background(ColorProvider(Color(0xFF14161A))).padding(12.dp)
                    .clickable(actionStartActivity(Intent(context, ReminderActivity::class.java))),
                verticalAlignment = Alignment.CenterVertically,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(st.service.label, style = TextStyle(color = ColorProvider(Color(0xFFA9A9B3)), fontSize = 12.sp))
                if (st.items.isEmpty()) {
                    Image(ImageProvider(R.drawable.ic_kotel), contentDescription = null, modifier = GlanceModifier.size(56.dp))
                } else {
                    st.items.take(4).forEach {
                        Text(it.heb, style = TextStyle(color = ColorProvider(Color(0xFFE2B84A)), fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center))
                    }
                }
            }
        }
    }
}

class DavenWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = DavenWidget()
}
