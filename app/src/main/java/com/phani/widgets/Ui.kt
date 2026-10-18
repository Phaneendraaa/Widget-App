package com.phani.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Looks like the widget itself, so what you design is what you get on the home screen. */
@Composable
fun CountdownCard(
    c: Countdown,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    actions: @Composable (RowScope.() -> Unit)? = null,
) {
    val st = Styles.get(c.style)
    val shown = c.shown()
    val pad = if (compact) 14.dp else 20.dp
    val bigSize = if (compact) 40.sp else 64.sp
    val capSize = if (compact) 12.sp else 16.sp
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (compact) 22.dp else 28.dp))
            .background(Color(st.bg))
            .padding(pad)
    ) {
        Text(
            c.title.uppercase(),
            color = Color(st.sub),
            fontSize = if (compact) 10.sp else 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                shown.big,
                color = Color(st.accent),
                fontSize = bigSize,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                shown.caption,
                color = Color(st.text),
                fontSize = capSize,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = if (compact) 8.dp else 14.dp),
            )
        }
        Text(c.dateLabel(), color = Color(st.sub), fontSize = if (compact) 11.sp else 13.sp)
        if (actions != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                content = actions,
            )
        }
    }
}
