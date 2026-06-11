package com.scotteth.skydivealtimeter

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * Converts barometric pressure (hPa) to altitude (meters) above standard sea level
 * using the international barometric formula.
 */
private fun pressureToAltitude(pressureHpa: Float): Float {
    return SensorManager.getAltitude(SensorManager.PRESSURE_STANDARD_ATMOSPHERE, pressureHpa)
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                AltimeterApp()
            }
        }
    }
}

@Composable
fun AltimeterApp() {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    }
    val pressureSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) }

    var currentPressure by remember { mutableFloatStateOf(SensorManager.PRESSURE_STANDARD_ATMOSPHERE) }
    var groundPressure by remember { mutableStateOf<Float?>(null) }
    var sensorAvailable by remember { mutableStateOf(pressureSensor != null) }
    var useMeters by remember { mutableStateOf(true) }

    DisposableEffect(sensorManager, pressureSensor) {
        if (pressureSensor == null) {
            sensorAvailable = false
            return@DisposableEffect onDispose {}
        }
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                currentPressure = event.values[0]
                if (groundPressure == null) {
                    groundPressure = currentPressure
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_FASTEST)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val absoluteAltitude = pressureToAltitude(currentPressure)
    val relativeAltitude = groundPressure?.let { ground ->
        absoluteAltitude - pressureToAltitude(ground)
    } ?: 0f

    // Page 0: main altimeter display. Swipe to page 1 for settings.
    val pagerState = rememberPagerState(pageCount = { 2 })

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> AltimeterScreen(
                relativeAltitude = relativeAltitude,
                useMeters = useMeters,
                sensorAvailable = sensorAvailable
            )
            else -> SettingsScreen(
                useMeters = useMeters,
                onUnitToggle = { useMeters = !useMeters },
                onZero = { groundPressure = currentPressure }
            )
        }
    }
}

@Composable
fun AltimeterScreen(
    relativeAltitude: Float,
    useMeters: Boolean,
    sensorAvailable: Boolean
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!sensorAvailable) {
            Text(text = "No barometer", fontSize = 16.sp)
        } else {
            val displayValue = if (useMeters) {
                relativeAltitude.toInt()
            } else {
                (relativeAltitude * 3.28084f).toInt()
            }
            val unitLabel = if (useMeters) "m" else "ft"
            val altitudeFt = relativeAltitude * 3.28084f
            val warningColor = if (altitudeFt < 2500f) Color.Red else Color.Unspecified

            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Scale the font relative to the screen size so the number
                // fills as much of the round watch face as possible.
                val numberFontSize = with(LocalDensity.current) { (maxWidth * 0.62f).toSp() }
                val unitFontSize = with(LocalDensity.current) { (maxWidth * 0.16f).toSp() }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "$displayValue",
                        fontSize = numberFontSize,
                        fontWeight = FontWeight.Bold,
                        color = warningColor,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                    Text(
                        text = unitLabel,
                        fontSize = unitFontSize,
                        color = warningColor,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(
    useMeters: Boolean,
    onUnitToggle: () -> Unit,
    onZero: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Settings", fontSize = 16.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onUnitToggle) {
            Text(text = if (useMeters) "Units: m" else "Units: ft")
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = onZero) {
            Text(text = "Zero")
        }
    }
}
