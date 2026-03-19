package com.example.metaglassesrecorder.wear.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.metaglassesrecorder.service.RecordingState

class WearMainActivity : ComponentActivity() {

    private val viewModel: WearMainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                val recordingState by viewModel.recordingState.collectAsState()
                val phoneReachable by viewModel.phoneReachable.collectAsState()
                WearRecorderScreen(
                    recordingState = recordingState,
                    phoneReachable = phoneReachable,
                    onButtonClick  = { viewModel.onVideoButtonPressed() }
                )
            }
        }
    }
}

@Composable
fun WearRecorderScreen(
    recordingState: RecordingState,
    phoneReachable: Boolean,
    onButtonClick: () -> Unit
) {
    val isRecording  = recordingState == RecordingState.RECORDING
    val isTransient  = recordingState == RecordingState.STARTING ||
                       recordingState == RecordingState.STOPPING

    val buttonColor by animateColorAsState(
        targetValue = when {
            isTransient  -> Color(0xFF888888)
            isRecording  -> Color(0xFFD50000)
            else         -> Color(0xFF1B5E20)
        },
        label = "buttonColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(8.dp)
        ) {
            if (isRecording) {
                RecordingPulse()
                Spacer(Modifier.height(6.dp))
            }

            Button(
                onClick  = onButtonClick,
                enabled  = !isTransient,
                modifier = Modifier.size(100.dp),
                colors   = ButtonDefaults.buttonColors(
                    backgroundColor         = buttonColor,
                    disabledBackgroundColor = Color(0xFF444444)
                )
            ) {
                Text(
                    text = when (recordingState) {
                        RecordingState.IDLE      -> "Start\nVideo"
                        RecordingState.STARTING  -> "Starting…"
                        RecordingState.RECORDING -> "Stop\nVideo"
                        RecordingState.STOPPING  -> "Stopping…"
                    },
                    fontSize    = 14.sp,
                    fontWeight  = FontWeight.Bold,
                    textAlign   = TextAlign.Center,
                    color       = Color.White
                )
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = when {
                    isRecording                                    -> "● REC"
                    !phoneReachable && recordingState == RecordingState.IDLE -> "Phone unreachable"
                    else                                           -> ""
                },
                fontSize  = 10.sp,
                color     = if (isRecording) Color.Red else Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun RecordingPulse() {
    val transition = rememberInfiniteTransition(label = "pulse")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue  = 0.15f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(Color.Red)
            .alpha(alpha)
    )
}
