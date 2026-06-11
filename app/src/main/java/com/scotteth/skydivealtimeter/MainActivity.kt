package com.scotteth.skydivealtimeter

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlin.math.pow

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
                AltimeterScreen()
            }
        }
    }
}

@Composable
fun AltimeterScreen() {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager
    }
    val pressureSensor = remember { sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE) }

    var currentPressure by remember { mutableFloatStateOf(SensorManager.PRESSURE_STANDARD_ATMOSPHERE) }
    var groundPressure by remember { mutableStateOf<Float?>(null) }
    var sensorAvailable by remember { mutableStateOf(pressureSensor != null) }

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
        sensorManager.registerListener(listener, pressureSensor, SensorManager.SENSOR_DELAY_UI)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val absoluteAltitude = pressureToAltitude(currentPressure)
    val relativeAltitude = groundPressure?.let { ground ->
        absoluteAltitude - pressureToAltitude(ground)
    } ?: 0f

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (!sensorAvailable) {
            Text(text = "No barometer", fontSize = 16.sp)
        } else {
            Text(text = "Altitude (AGL)", fontSize = 12.sp)
            Text(
                text = "${relativeAltitude.toInt()} m",
                fontSize = 32.sp
            )
            Text(
                text = "${(relativeAltitude * 3.28084f).toInt()} ft",
                fontSize = 16.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { groundPressure = currentPressure }) {
                Text(text = "Zero")
            }
        }
    }
}
