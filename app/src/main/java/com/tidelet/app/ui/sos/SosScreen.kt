package com.tidelet.app.ui.sos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tidelet.app.R
import com.tidelet.app.ui.nav.Routes
import com.tidelet.app.ui.theme.TideletTheme

@Composable
fun SosScreen(
    onClose: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    // Warm background for the entire SOS flow.
    val sosSurface = TideletTheme.extended.sosSurface
    val onSos = TideletTheme.extended.onSosSurface

    Box(
        modifier = Modifier.fillMaxSize().background(sosSurface),
    ) {
        // Close X in the top-right
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
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(72.dp))
            Text(
                text = stringResource(R.string.sos_title),
                style = MaterialTheme.typography.headlineSmall,
                color = onSos,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            // Clean 2x2 grid — four tools, equal weight.
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                item {
                    SosTile(
                        icon = Icons.Rounded.Waves,
                        titleRes = R.string.sos_ride_wave,
                        subtitleRes = R.string.sos_ride_wave_sub,
                        onClick = { onNavigate(Routes.SOS_WAVE) },
                        tileTag = "sos_grid_ride_wave",
                    )
                }
                item {
                    SosTile(
                        icon = Icons.Rounded.Air,
                        titleRes = R.string.sos_breathe,
                        subtitleRes = R.string.sos_breathe_sub,
                        onClick = { onNavigate(Routes.SOS_BREATHE) },
                        tileTag = "sos_grid_breathe",
                    )
                }
                item {
                    SosTile(
                        icon = Icons.Rounded.Favorite,
                        titleRes = R.string.sos_reasons,
                        subtitleRes = R.string.sos_reasons_sub,
                        onClick = { onNavigate(Routes.SOS_REASONS) },
                        tileTag = "sos_grid_reasons",
                    )
                }
                item {
                    SosTile(
                        icon = Icons.Rounded.SelfImprovement,
                        titleRes = R.string.sos_distractions,
                        subtitleRes = R.string.sos_distractions_sub,
                        onClick = { onNavigate(Routes.SOS_DISTRACTIONS) },
                        tileTag = "sos_grid_distractions",
                    )
                }
                // Full-width "last rung" tile — Journal is reached for when
                // the other tools haven't landed. Framed as calmer, wider,
                // and a touch shorter than the 2x2 tiles above it.
                item(span = { GridItemSpan(maxLineSpan) }) {
                    SosWideTile(
                        icon = Icons.Rounded.Edit,
                        titleRes = R.string.sos_journal,
                        subtitleRes = R.string.sos_journal_sub,
                        onClick = { onNavigate(Routes.SOS_JOURNAL) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SosTile(
    icon: ImageVector,
    titleRes: Int,
    subtitleRes: Int,
    onClick: () -> Unit,
    tileTag: String = "",
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().aspectRatio(1f)
            .let { if (tileTag.isNotEmpty()) it.testTag(tileTag) else it },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TideletTheme.extended.onSosSurface,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(icon, contentDescription = null, tint = TideletTheme.extended.sosAccent)
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Wide variant of [SosTile] for the Journal row. Same card styling, but
 * no aspect-ratio lock (it lives alone on the line) and icon + text laid
 * out horizontally so the tile reads as a calmer "last rung" rather than
 * another peer to the 2x2 grid above.
 */
@Composable
private fun SosWideTile(
    icon: ImageVector,
    titleRes: Int,
    subtitleRes: Int,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = TideletTheme.extended.onSosSurface,
        ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = TideletTheme.extended.sosAccent)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(titleRes),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(subtitleRes),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
