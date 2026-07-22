package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BrandLockup(title: String = "LoopTrack", subtitle: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(modifier = Modifier.size(40.dp)) {
            drawCircle(color = Color.White.copy(alpha = 0.06f), radius = size.minDimension / 2f)
            drawArc(
                color = Color(0xFFE4C26A),
                startAngle = 210f,
                sweepAngle = 120f,
                useCenter = false,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
            drawArc(
                color = Color(0xFF62F27A),
                startAngle = 350f,
                sweepAngle = 170f,
                useCenter = false,
                style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Column {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun PremiumActionTile(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    supporting: String? = null,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        accent.copy(alpha = 0.92f),
                        accent.copy(alpha = 0.55f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                ),
                shape
            )
            .border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(onClick = onClick)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(30.dp))
        Text(label, color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        supporting?.let {
            Text(it, color = Color.White.copy(alpha = 0.78f), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
fun Modifier.loopTrackBackground(): Modifier {
    return background(
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF07131B),
                MaterialTheme.colorScheme.background,
                Color(0xFF020506)
            )
        )
    )
}

@Composable
fun LoopCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                            Color(0xFF091116)
                        )
                    )
                )
                .padding(18.dp),
            content = content
        )
    }
}

@Composable
fun ConfidenceBadge(value: String, modifier: Modifier = Modifier) {
    val normalized = value.uppercase()
    val color = when (normalized) {
        "HIGH" -> MaterialTheme.colorScheme.primary
        "MEDIUM" -> MaterialTheme.colorScheme.tertiary
        "LOW" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.secondary
    }
    Text(
        text = normalized.lowercase().replaceFirstChar { it.uppercase() },
        modifier = modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
            .border(1.dp, color.copy(alpha = 0.42f), RoundedCornerShape(999.dp))
            .padding(horizontal = 11.dp, vertical = 6.dp),
        color = color,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
fun MetricTile(label: String, value: String, modifier: Modifier = Modifier, supporting: String? = null) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.045f), RoundedCornerShape(10.dp))
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        supporting?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
        }
    }
}

@Composable
fun StatusRow(label: String, value: String, ready: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary, CircleShape)
                    .border(2.dp, Color.White.copy(alpha = 0.12f), CircleShape)
            )
            Text(label, color = MaterialTheme.colorScheme.onSurface)
        }
        Text(value, color = if (ready) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
fun SchematicLoop(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.secondary,
    showMarker: Boolean = true
) {
    val markerColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val strokeWidth = 9.dp.toPx()
        val width = size.width
        val height = size.height
        val pathLeft = strokeWidth * 1.5f
        val pathTop = height * 0.18f
        val pathSize = Size(width - strokeWidth * 3f, height * 0.62f)

        drawArc(
            color = accent.copy(alpha = 0.12f),
            startAngle = 10f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(pathLeft, pathTop),
            size = pathSize,
            style = Stroke(width = strokeWidth * 1.9f, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color.White.copy(alpha = 0.16f),
            startAngle = 10f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(pathLeft, pathTop),
            size = pathSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = accent,
            startAngle = 10f,
            sweepAngle = 360f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = Offset(pathLeft, pathTop),
            size = pathSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        if (showMarker) {
            drawCircle(
                color = markerColor.copy(alpha = 0.20f),
                radius = 14.dp.toPx(),
                center = Offset(width * 0.78f, height * 0.48f)
            )
            drawCircle(
                color = markerColor,
                radius = 6.dp.toPx(),
                center = Offset(width * 0.78f, height * 0.48f)
            )
        }
    }
}

fun loopTrackTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s) else String.format("%02d:%02d", m, s)
}
