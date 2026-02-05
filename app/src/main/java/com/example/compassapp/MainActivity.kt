package com.example.compassapp

import android.content.Context
import android.hardware.*
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator


val AppleRed = Color(0xFFFF3B30)
val AppleGray = Color(0xFF8E8E93)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            androidx.compose.material3.MaterialTheme(
                colorScheme = androidx.compose.material3.darkColorScheme()
            ) {
                CompassScreen()
            }
        }
    }
}

@Composable
fun CompassScreen() {

    val context = LocalContext.current
    val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    var hasVibrated by remember { mutableStateOf(false) }


    // If device doesn't support compass → show message, NO sensors used
    if (accelerometer == null || magnetometer == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Compass not supported on this device",
                fontSize = 18.sp
            )
        }
        return
    }

    var accelData by remember { mutableStateOf(FloatArray(3)) }
    var magnetData by remember { mutableStateOf(FloatArray(3)) }
    var degree by remember { mutableStateOf(0f) }

    DisposableEffect(Unit) {

        val listener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> accelData = event.values.clone()
                    Sensor.TYPE_MAGNETIC_FIELD -> magnetData = event.values.clone()
                }

                val r = FloatArray(9)
                val orientation = FloatArray(3)

                if (SensorManager.getRotationMatrix(r, null, accelData, magnetData)) {
                    SensorManager.getOrientation(r, orientation)
                    val azimuth =
                        Math.toDegrees(orientation[0].toDouble()).toFloat()
                    degree = smoothAngle(degree, (azimuth + 360) % 360)

                }
                val rounded = degree.roundToInt()

                if (rounded in 358..360 || rounded in 0..1) vibrator?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        it.vibrate(
                            VibrationEffect.createOneShot(
                                20,
                                VibrationEffect.DEFAULT_AMPLITUDE
                            )
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        it.vibrate(20)
                    }
                }
                else {
                    hasVibrated = false
                }

            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        sensorManager.registerListener(
            listener,
            magnetometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ){

        // Big degree (Apple style)
        Text(
            text = "${degree.roundToInt()}°",
            fontSize = 64.sp,
            color = Color.White
        )

        // Direction (small & subtle)
        Text(
            text = getDirection(degree.roundToInt()),
            fontSize = 18.sp,
            color = Color(0xFF8E8E93) // Apple gray
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Compass ring + needle
        Box(contentAlignment = Alignment.Center) {

            CompassRing()

            Image(
                painter = painterResource(id = R.drawable.compass_needle),
                contentDescription = "Needle",
                modifier = Modifier
                    .size(140.dp)
                    .rotate(-degree)
            )
        }
    }
}
@Composable
fun CompassRing() {
    Box(
        modifier = Modifier.size(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "N", fontSize = 18.sp, color = AppleRed,
            modifier = Modifier.offset(y = (-120).dp)
        )
        Text(
            "S", fontSize = 18.sp, color = Color.White,
            modifier = Modifier.offset(y = 120.dp)
        )
        Text(
            "E", fontSize = 18.sp, color = Color.White,
            modifier = Modifier.offset(x = 120.dp)
        )
        Text(
            "W", fontSize = 18.sp, color = Color.White,
            modifier = Modifier.offset(x = (-120).dp)
        )
    }
}


fun getDirection(deg: Int): String {
    return when (deg) {
        in 348..360, in 0..11 -> "N"
        in 12..33 -> "NNE"
        in 34..56 -> "NE"
        in 57..78 -> "ENE"
        in 79..101 -> "E"
        in 102..123 -> "ESE"
        in 124..146 -> "SE"
        in 147..168 -> "SSE"
        in 169..191 -> "S"
        in 192..213 -> "SSW"
        in 214..236 -> "SW"
        in 237..258 -> "WSW"
        in 259..281 -> "W"
        in 282..303 -> "WNW"
        in 304..326 -> "NW"
        in 327..347 -> "NNW"
        else -> "Unknown"
    }
}

fun smoothAngle(old: Float, new: Float, alpha: Float = 0.15f): Float {
    var diff = new - old
    if (diff > 180) diff -= 360f
    if (diff < -180) diff += 360f
    return (old + alpha * diff + 360) % 360
}