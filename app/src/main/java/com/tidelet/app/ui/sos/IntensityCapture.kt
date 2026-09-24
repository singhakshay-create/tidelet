package com.tidelet.app.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tidelet.app.R
import com.tidelet.app.ui.theme.TideletTheme
import kotlin.math.roundToInt

@Composable
fun IntensityCapture(
    onIntensitySelected: (Int) -> Unit,
    onSkip: () -> Unit,
    onClose: () -> Unit,
) {
    val sosSurface = TideletTheme.extended.sosSurface
    val onSos = TideletTheme.extended.onSosSurface

    var sliderValue by remember { mutableFloatStateOf(5f) }

    Box(
        modifier = Modifier.fillMaxSize().background(sosSurface),
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.sos_close),
                tint = onSos,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.intensity_title),
                style = MaterialTheme.typography.headlineSmall,
                color = onSos,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.intensity_body),
                style = MaterialTheme.typography.bodyMedium,
                color = onSos,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(40.dp))

            Text(
                text = "${sliderValue.roundToInt()}",
                style = MaterialTheme.typography.displayMedium,
                color = onSos,
            )

            Spacer(Modifier.height(16.dp))

            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                valueRange = 1f..10f,
                steps = 8,
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = TideletTheme.extended.sosAccent,
                    activeTrackColor = TideletTheme.extended.sosAccent,
                ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(R.string.intensity_low),
                    style = MaterialTheme.typography.labelSmall,
                    color = onSos.copy(alpha = 0.7f),
                )
                Text(
                    text = stringResource(R.string.intensity_high),
                    style = MaterialTheme.typography.labelSmall,
                    color = onSos.copy(alpha = 0.7f),
                )
            }

            Spacer(Modifier.height(40.dp))

            Button(
                onClick = { onIntensitySelected(sliderValue.roundToInt()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.intensity_continue))
            }

            Spacer(Modifier.height(12.dp))

            TextButton(onClick = onSkip) {
                Text(
                    text = stringResource(R.string.intensity_skip),
                    color = onSos.copy(alpha = 0.7f),
                )
            }
        }
    }
}
