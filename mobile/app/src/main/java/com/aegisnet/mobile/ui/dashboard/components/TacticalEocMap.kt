package com.aegisnet.mobile.ui.dashboard.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aegisnet.mobile.domain.model.CrisisAlert
import com.aegisnet.mobile.domain.model.DroneStatus
import com.aegisnet.mobile.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.abs

// Earth distance helper (Haversine formula)
fun calculateDistanceKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

@Composable
fun TacticalEocMap(
    alerts: List<CrisisAlert>,
    drones: List<DroneStatus>,
    modifier: Modifier = Modifier,
    onAlertClick: ((CrisisAlert) -> Unit)? = null
) {
    var mapOffset by remember { mutableStateOf(Offset.Zero) }
    var selectedAlert by remember(alerts) { mutableStateOf<CrisisAlert?>(null) }
    var zoomScale by remember { mutableStateOf(1.0f) }

    // Pulsating warning boundary animation
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadiusScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radius"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    // Radar scanning line animation
    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "radar"
    )

    // Drone flight vector progress animation
    val droneProgress by infiniteTransition.animateFloat(
        initialValue = 0.0f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(10000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "droneFly"
    )

    // Dynamic map center based on first active alert or fallback to Rawalpindi EOC HQ
    val baseLat = alerts.firstOrNull { it.epicenterLat != null && it.epicenterLat != 0.0 }?.epicenterLat ?: 33.6844
    val baseLng = alerts.firstOrNull { it.epicenterLng != null && it.epicenterLng != 0.0 }?.epicenterLng ?: 73.0479

    // Scale factor to fit all active incidents inside the scanning radar concentric circles dynamically (autoscale)
    val baseScaleFactor = 1600f
    val scaleFactor = remember(alerts, baseLat, baseLng) {
        if (alerts.isEmpty()) {
            baseScaleFactor
        } else {
            var maxDelta = 0.001 // Prevent division by zero
            alerts.forEach { alert ->
                val lat = alert.epicenterLat ?: return@forEach
                val lng = alert.epicenterLng ?: return@forEach
                val deltaLat = abs(lat - baseLat)
                val deltaLng = abs(lng - baseLng)
                if (deltaLat > maxDelta) maxDelta = deltaLat
                if (deltaLng > maxDelta) maxDelta = deltaLng
            }
            // Radius of outer ring is 290.dp * 0.46f. Target approx 130 pixels offset inside map boundaries.
            val targetPixels = 130f
            (targetPixels / maxDelta).toFloat().coerceIn(1600f, 15000f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(290.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AegisDark)
            .border(BorderStroke(1.dp, AegisSlate), RoundedCornerShape(12.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(alerts, baseLat, baseLng, mapOffset, scaleFactor, zoomScale) {
                    detectTapGestures { tapOffset ->
                        val width = size.width
                        val height = size.height
                        val center = Offset(width / 2f, height / 2f) + mapOffset

                        var clickedAlert: CrisisAlert? = null
                        var minDistance = Float.MAX_VALUE

                        alerts.forEach { alert ->
                            val lat = alert.epicenterLat ?: return@forEach
                            val lng = alert.epicenterLng ?: return@forEach
                            val projectedX = ((lng - baseLng) * scaleFactor * zoomScale).toFloat()
                            val projectedY = (-(lat - baseLat) * scaleFactor * zoomScale).toFloat()
                            val alertCenter = center + Offset(projectedX, projectedY)

                            val dist = (tapOffset - alertCenter).getDistance()
                            if (dist < 24.dp.toPx() && dist < minDistance) {
                                clickedAlert = alert
                                minDistance = dist
                            }
                        }
                        selectedAlert = clickedAlert
                        clickedAlert?.let { onAlertClick?.invoke(it) }
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(0.5f, 8.0f)
                        mapOffset = (mapOffset - centroid) * zoom + centroid + pan
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val center = Offset(width / 2, height / 2) + mapOffset

            // 1. Draw Grid backdrop
            val gridSize = 40.dp.toPx()
            val startX = (mapOffset.x % gridSize)
            val startY = (mapOffset.y % gridSize)

            // Draw vertical grid lines
            var x = startX
            while (x < width) {
                drawLine(
                    color = AegisSlate.copy(alpha = 0.12f),
                    start = Offset(x, 0f),
                    end = Offset(x, height),
                    strokeWidth = 1.dp.toPx()
                )
                x += gridSize
            }

            // Draw horizontal grid lines
            var y = startY
            while (y < height) {
                drawLine(
                    color = AegisSlate.copy(alpha = 0.12f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
                y += gridSize
            }

            // 2. Draw Radar concentric circular grids (cyber center radar rings)
            val maxRadarRadius = kotlin.math.min(width, height) * 0.46f
            
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.argb(100, 0, 229, 255) // AegisPrimary with alpha
                textSize = 8.sp.toPx()
                typeface = android.graphics.Typeface.MONOSPACE
                textAlign = android.graphics.Paint.Align.CENTER
            }

            for (i in 1..4) {
                val ringRadius = maxRadarRadius * (i / 4f)
                drawCircle(
                    color = AegisPrimary.copy(alpha = 0.08f),
                    radius = ringRadius,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Render beautiful Range distance values on concentric rings
                val textY = center.y - ringRadius + 10.dp.toPx()
                drawContext.canvas.nativeCanvas.drawText(
                    "RNG: ${10 * i}KM",
                    center.x,
                    textY,
                    paint
                )
            }

            // Draw tactical sweeping line
            val angleRad = Math.toRadians(radarAngle.toDouble())
            val lineEnd = Offset(
                x = center.x + maxRadarRadius * cos(angleRad).toFloat(),
                y = center.y + maxRadarRadius * sin(angleRad).toFloat()
            )
            drawLine(
                color = AegisPrimary.copy(alpha = 0.35f),
                start = center,
                end = lineEnd,
                strokeWidth = 1.5.dp.toPx()
            )

            // Draw cardinal compass indicators
            val compassPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                textSize = 9.sp.toPx()
                typeface = android.graphics.Typeface.create(android.graphics.Typeface.MONOSPACE, android.graphics.Typeface.BOLD)
                textAlign = android.graphics.Paint.Align.CENTER
            }
            // North
            drawContext.canvas.nativeCanvas.drawText("N", center.x, center.y - maxRadarRadius - 4.dp.toPx(), compassPaint)
            // South
            drawContext.canvas.nativeCanvas.drawText("S", center.x, center.y + maxRadarRadius + 11.dp.toPx(), compassPaint)
            // East
            drawContext.canvas.nativeCanvas.drawText("E", center.x + maxRadarRadius + 8.dp.toPx(), center.y + 3.dp.toPx(), compassPaint)
            // West
            drawContext.canvas.nativeCanvas.drawText("W", center.x - maxRadarRadius - 8.dp.toPx(), center.y + 3.dp.toPx(), compassPaint)

            // 3. Draw EOC base command center hub
            drawCircle(
                color = AegisPrimary,
                radius = 6.dp.toPx(),
                center = center
            )
            drawCircle(
                color = AegisPrimary.copy(alpha = 0.2f),
                radius = 12.dp.toPx(),
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 4. Plot dynamic Crisis Alert epicenters
            alerts.forEachIndexed { index, alert ->
                val lat = alert.epicenterLat ?: return@forEachIndexed
                val lng = alert.epicenterLng ?: return@forEachIndexed

                // Map real GPS coordinates relative to map center
                val projectedX = ((lng - baseLng) * scaleFactor * zoomScale).toFloat()
                val projectedY = (-(lat - baseLat) * scaleFactor * zoomScale).toFloat() // Invert Y
                val alertCenter = center + Offset(projectedX, projectedY)

                val riskColor = when {
                    alert.casualtyRiskScore >= 80 -> AegisAlert
                    alert.casualtyRiskScore >= 50 -> AegisWarn
                    else -> AegisSuccess
                }

                // Compute alert bearing angle from radar center in degrees
                val alertAngleRad = atan2(projectedY, projectedX)
                var alertAngleDeg = Math.toDegrees(alertAngleRad.toDouble()).toFloat()
                if (alertAngleDeg < 0) alertAngleDeg += 360f

                // Compute scanning sweeping overlap decay
                val angleDiff = (radarAngle - alertAngleDeg + 360f) % 360f
                val sweepDecay = if (angleDiff in 0f..140f) {
                    1f - (angleDiff / 140f)
                } else {
                    0.0f
                }

                // Dynamic sonar wave ripple expansion
                if (sweepDecay > 0.05f) {
                    drawCircle(
                        color = riskColor.copy(alpha = sweepDecay * 0.4f),
                        radius = 8.dp.toPx() + 32.dp.toPx() * (1f - sweepDecay),
                        center = alertCenter,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                }

                // Base dot brightness boosts with sweep overlap
                val dotAlpha = 0.5f + (sweepDecay * 0.5f)
                val isSelected = selectedAlert?.id == alert.id

                if (isSelected) {
                    // Lock-on cyan target indicators
                    drawCircle(
                        color = AegisPrimary,
                        radius = 16.dp.toPx(),
                        center = alertCenter,
                        style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f))
                    )
                    drawLine(
                        color = AegisPrimary.copy(alpha = 0.4f),
                        start = center,
                        end = alertCenter,
                        strokeWidth = 1.5.dp.toPx()
                    )
                }

                // Inner core warning point
                drawCircle(
                    color = riskColor.copy(alpha = dotAlpha),
                    radius = if (isSelected) 9.dp.toPx() else 7.dp.toPx(),
                    center = alertCenter
                )

                // Concentric warning rings (pulsating)
                drawCircle(
                    color = riskColor.copy(alpha = pulseAlpha * dotAlpha),
                    radius = 26.dp.toPx() * pulseRadiusScale,
                    center = alertCenter,
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Draw connection lines to consecutive incidents (evacuation/triage paths)
                if (index > 0) {
                    val prevAlert = alerts[index - 1]
                    val prevLat = prevAlert.epicenterLat
                    val prevLng = prevAlert.epicenterLng
                    if (prevLat != null && prevLng != null) {
                        val prevX = ((prevLng - baseLng) * scaleFactor * zoomScale).toFloat()
                        val prevY = (-(prevLat - baseLat) * scaleFactor * zoomScale).toFloat()
                        val prevCenter = center + Offset(prevX, prevY)

                        drawLine(
                            color = AegisSuccess.copy(alpha = 0.2f),
                            start = prevCenter,
                            end = alertCenter,
                            strokeWidth = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                    }
                }

                // --- 5. Draw Dynamic Google Places Swept Infrastructure Markers ---
                // Hospitals (green crosses)
                alert.nearbyHospitals.forEachIndexed { hIndex, _ ->
                    val angle = (hIndex * 360f / alert.nearbyHospitals.size.coerceAtLeast(1)) * (Math.PI / 180f)
                    val radius = 24.dp.toPx()
                    val hospOffset = alertCenter + Offset(
                        x = (radius * cos(angle)).toFloat(),
                        y = (radius * sin(angle)).toFloat()
                    )

                    // Draw connecting telemetry thread
                    drawLine(
                        color = AegisSuccess.copy(alpha = 0.25f),
                        start = alertCenter,
                        end = hospOffset,
                        strokeWidth = 0.8.dp.toPx()
                    )

                    // Draw hospital green cross marker
                    drawCircle(
                        color = AegisSuccess.copy(alpha = dotAlpha),
                        radius = 4.dp.toPx(),
                        center = hospOffset
                    )

                    // Draw simple "+" indicator
                    drawLine(
                        color = Color.White.copy(alpha = dotAlpha),
                        start = Offset(hospOffset.x - 2.dp.toPx(), hospOffset.y),
                        end = Offset(hospOffset.x + 2.dp.toPx(), hospOffset.y),
                        strokeWidth = 0.8.dp.toPx()
                    )
                    drawLine(
                        color = Color.White.copy(alpha = dotAlpha),
                        start = Offset(hospOffset.x, hospOffset.y - 2.dp.toPx()),
                        end = Offset(hospOffset.x, hospOffset.y + 2.dp.toPx()),
                        strokeWidth = 0.8.dp.toPx()
                    )
                }

                // Police (blue shields/squares)
                alert.nearbyPolice.forEachIndexed { pIndex, _ ->
                    val angle = ((pIndex * 360f / alert.nearbyPolice.size.coerceAtLeast(1)) + 180f) * (Math.PI / 180f)
                    val radius = 28.dp.toPx()
                    val policeOffset = alertCenter + Offset(
                        x = (radius * cos(angle)).toFloat(),
                        y = (radius * sin(angle)).toFloat()
                    )

                    // Draw connecting telemetry thread
                    drawLine(
                        color = AegisPrimary.copy(alpha = 0.25f),
                        start = alertCenter,
                        end = policeOffset,
                        strokeWidth = 0.8.dp.toPx()
                    )

                    // Draw police indicator (diamond/shield)
                    val shieldPath = Path().apply {
                        moveTo(policeOffset.x, policeOffset.y - 4.dp.toPx())
                        lineTo(policeOffset.x + 4.dp.toPx(), policeOffset.y)
                        lineTo(policeOffset.x, policeOffset.y + 4.dp.toPx())
                        lineTo(policeOffset.x - 4.dp.toPx(), policeOffset.y)
                        close()
                    }
                    drawPath(
                        path = shieldPath,
                        color = AegisPrimary.copy(alpha = dotAlpha)
                    )
                }
            }

            // 6. Draw dynamic flying Drones along vector paths from EOC Hub to targets
            drones.forEachIndexed { index, drone ->
                if (alerts.isEmpty()) {
                    // Fallback to orbiting EOC center
                    val orbitAngle = (droneProgress * 360f + (index * 180f)) * (Math.PI / 180f)
                    val orbitRadius = 45.dp.toPx()
                    val fallbackOffset = center + Offset(
                        (orbitRadius * cos(orbitAngle)).toFloat(),
                        (orbitRadius * sin(orbitAngle)).toFloat()
                    )

                    drawDroneMarker(fallbackOffset, AegisPrimary)
                    return@forEachIndexed
                }

                // Fly towards assigned alert epicenter
                val targetAlert = alerts[index % alerts.size]
                val targetLat = targetAlert.epicenterLat ?: baseLat
                val targetLng = targetAlert.epicenterLng ?: baseLng

                val targetX = ((targetLng - baseLng) * scaleFactor * zoomScale).toFloat()
                val targetY = (-(targetLat - baseLat) * scaleFactor * zoomScale).toFloat()
                val targetOffset = center + Offset(targetX, targetY)

                // Linear interpolation for flying progress
                val droneX = center.x + (targetOffset.x - center.x) * droneProgress
                val droneY = center.y + (targetOffset.y - center.y) * droneProgress
                val droneOffset = Offset(droneX, droneY)

                // Drone tracking flight line
                drawLine(
                    color = AegisPrimary.copy(alpha = 0.15f),
                    start = center,
                    end = targetOffset,
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                )

                val droneColor = if (drone.status == "SCANNING") AegisSuccess else AegisPrimary

                // Rotate the drone vector to point towards its active flight coordinate target
                val dx = targetOffset.x - center.x
                val dy = targetOffset.y - center.y
                val droneAngleRad = atan2(dy, dx)

                withTransform({
                    rotate(degrees = Math.toDegrees(droneAngleRad.toDouble()).toFloat() + 90f, pivot = droneOffset)
                }) {
                    drawDroneMarker(droneOffset, droneColor)
                }

                // Drone scanning radar pulse
                drawCircle(
                    color = droneColor.copy(alpha = 0.15f),
                    radius = 12.dp.toPx() * (1f + (droneProgress * 0.5f)),
                    center = droneOffset,
                    style = Stroke(width = 1.dp.toPx())
                )

                // Render micro drone ID & battery text next to the marker
                val droneTextPaint = android.graphics.Paint().apply {
                    color = if (drone.status == "SCANNING") 0xFF05FF80.toInt() else 0xFF00E5FF.toInt()
                    textSize = 7.sp.toPx()
                    typeface = android.graphics.Typeface.MONOSPACE
                }
                drawContext.canvas.nativeCanvas.drawText(
                    "${drone.droneId} [${drone.batteryLevel}%]",
                    droneOffset.x + 8.dp.toPx(),
                    droneOffset.y + 3.dp.toPx(),
                    droneTextPaint
                )
            }
        }

        // Overlay EOC system logs / metrics HUD
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 54.dp)
                .background(AegisPanel.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .border(BorderStroke(0.5.dp, AegisSlate), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "TACTICAL RADAR SYNC",
                    color = AegisPrimary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "CENTER: %.4f°N / %.4f°E".format(baseLat, baseLng),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 8.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "ZOOM: %.1fx".format(zoomScale),
                        color = AegisSuccess,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        // Map Legend Indicators
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(12.dp)
                .background(AegisPanel.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .border(BorderStroke(0.5.dp, AegisSlate), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                LegendItem("DRONE", AegisPrimary)
                LegendItem("INCIDENT", AegisAlert)
                LegendItem("HOSPITAL", AegisSuccess)
                LegendItem("POLICE", Color(0xFF00E5FF))
            }
        }

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .background(AegisPanel.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                .border(BorderStroke(0.5.dp, AegisSlate), RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "TAP ALERT TO VIEW SPEC / EOC ACTIVE",
                color = AegisSuccess,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // --- Beautiful Floating Glassmorphism Alert Info Card Overlay ---
        AnimatedVisibility(
            visible = selectedAlert != null,
            enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
            exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp, top = 12.dp, bottom = 12.dp)
        ) {
            selectedAlert?.let { alert ->
                Card(
                    modifier = Modifier
                        .width(190.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(AegisPanel.copy(alpha = 0.95f))
                        .border(BorderStroke(1.dp, AegisPrimary.copy(alpha = 0.6f)), RoundedCornerShape(8.dp)),
                    colors = CardDefaults.cardColors(containerColor = AegisPanel.copy(alpha = 0.95f))
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = alert.type.replace("_", " "),
                                color = when {
                                    alert.casualtyRiskScore >= 80 -> AegisAlert
                                    alert.casualtyRiskScore >= 50 -> AegisWarn
                                    else -> AegisSuccess
                                },
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            IconButton(
                                onClick = { selectedAlert = null },
                                modifier = Modifier.size(18.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }

                        Text(
                            text = alert.title,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )

                        Text(
                            text = alert.description,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 8.sp,
                            lineHeight = 11.sp,
                            maxLines = 3
                        )

                        Divider(color = AegisSlate.copy(alpha = 0.5f), thickness = 0.5.dp, modifier = Modifier.padding(vertical = 2.dp))

                        val distance = calculateDistanceKm(
                            baseLat, baseLng,
                            alert.epicenterLat ?: baseLat, alert.epicenterLng ?: baseLng
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DISTANCE:", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                            Text("%.2f KM".format(distance), color = AegisPrimary, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("DRN ETA:", color = Color.Gray, fontSize = 7.sp, fontFamily = FontFamily.Monospace)
                            val eta = (distance * 1.5).coerceIn(1.0, 30.0) // approx flight minutes
                            Text("%.1f MIN".format(eta), color = AegisSuccess, fontSize = 7.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        if (alert.placeName != null) {
                            Text(
                                text = "📍 ${alert.placeName}",
                                color = AegisPrimary,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                        }

                        if (alert.nearbyHospitals.isNotEmpty() || alert.nearbyPolice.isNotEmpty()) {
                            Text(
                                text = "🏥 ${alert.nearbyHospitals.size} Hosp | 🚔 ${alert.nearbyPolice.size} Police",
                                color = AegisSuccess,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDroneMarker(offset: Offset, color: Color) {
    val path = Path().apply {
        moveTo(offset.x, offset.y - 7.dp.toPx())
        lineTo(offset.x - 5.dp.toPx(), offset.y + 5.dp.toPx())
        lineTo(offset.x, offset.y + 2.dp.toPx())
        lineTo(offset.x + 5.dp.toPx(), offset.y + 5.dp.toPx())
        close()
    }
    drawPath(
        path = path,
        color = color
    )
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color)
        )
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 7.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold
        )
    }
}
