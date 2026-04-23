package com.tidelet.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Settings — the landing spot for everything that isn't a bottom-nav or
 * SOS-tier screen. We deliberately keep this as a single list of rows (no
 * tabs, no sub-pages beyond the ones they navigate to) so adding the CBT
 * tools here doesn't grow the information architecture.
 *
 * Groups shown:
 *
 *   1. **Plans & practice** — Relapse plan, Refusal rehearsal, Distortions
 *      library. These are the "read/edit in a calm moment" CBT surfaces that
 *      don't live on the SOS grid (which is for in-craving moments).
 *   2. **Your data** — Export (Markdown), Import (Markdown), Delete all.
 *
 * Pickers use the Storage Access Framework so the file is written wherever
 * the user likes — no permission prompts required.
 */
@Composable
fun SettingsScreen(
    onOpenRelapsePlan: () -> Unit = {},
    onOpenRefusalRehearsal: () -> Unit = {},
    onOpenDistortionsLibrary: () -> Unit = {},
    vm: SettingsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsStateWithLifecycle()

    // --- SAF launchers ---
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown"),
    ) { uri ->
        if (uri != null) vm.exportTo(context.contentResolver, uri)
    }
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) vm.importFrom(context.contentResolver, uri)
    }

    var confirmWipe by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
        ) {
            Text(
                text = stringResource(R.string.nav_settings),
                style = MaterialTheme.typography.headlineSmall,
            )

            Spacer(Modifier.height(16.dp))

            // --- Plans & practice ---
            SectionLabel(stringResource(R.string.settings_section_plans))
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Shield,
                    title = stringResource(R.string.settings_relapse_plan),
                    subtitle = stringResource(R.string.settings_relapse_plan_sub),
                    onClick = onOpenRelapsePlan,
                )
                SettingsRow(
                    icon = Icons.Filled.RecordVoiceOver,
                    title = stringResource(R.string.settings_refusal),
                    subtitle = stringResource(R.string.settings_refusal_sub),
                    onClick = onOpenRefusalRehearsal,
                )
                SettingsRow(
                    icon = Icons.Filled.Lightbulb,
                    title = stringResource(R.string.settings_distortions),
                    subtitle = stringResource(R.string.settings_distortions_sub),
                    onClick = onOpenDistortionsLibrary,
                    isLast = true,
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- Drinking baseline ---
            SectionLabel(stringResource(R.string.settings_section_baseline))
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                BaselineEditor(
                    drinksPerDay = state.profile?.typicalDrinksPerDay ?: 3,
                    priceCents = state.profile?.priceCentsPerDrink ?: 800,
                    hoursPerDrink = state.profile?.hoursPerDrink ?: 1,
                    onSave = vm::updateBaseline,
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- Reflection (PHASE_2 §9A5 — evening mini-review opt-in,
            //                  §9B1 — visual companion opt-in) ---
            SectionLabel(stringResource(R.string.settings_section_reflection))
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                EveningReviewToggleRow(
                    enabled = state.profile?.eveningReviewEnabled ?: false,
                    onChange = vm::setEveningReviewEnabled,
                )
                CompanionToggleRow(
                    enabled = state.profile?.companionEnabled ?: false,
                    onChange = vm::setCompanionEnabled,
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- Your data ---
            SectionLabel(stringResource(R.string.settings_section_data))
            Spacer(Modifier.height(8.dp))
            SettingsCard {
                SettingsRow(
                    icon = Icons.Filled.Download,
                    title = stringResource(R.string.settings_export),
                    subtitle = stringResource(R.string.settings_export_sub),
                    onClick = {
                        val suggestion = "tidelet-${LocalDate.now().format(DateTimeFormatter.ISO_DATE)}.md"
                        exportLauncher.launch(suggestion)
                    },
                )
                SettingsRow(
                    icon = Icons.Filled.Upload,
                    title = stringResource(R.string.settings_import),
                    subtitle = stringResource(R.string.settings_import_sub),
                    onClick = {
                        importLauncher.launch(arrayOf("text/markdown", "text/plain", "*/*"))
                    },
                )
                SettingsRow(
                    icon = Icons.Filled.VerifiedUser,
                    title = stringResource(R.string.settings_verify_backup),
                    subtitle = stringResource(R.string.settings_verify_backup_sub),
                    onClick = { vm.verifyBackup() },
                )
                SettingsRow(
                    icon = Icons.Filled.DeleteForever,
                    title = stringResource(R.string.settings_wipe),
                    subtitle = stringResource(R.string.settings_wipe_sub),
                    onClick = { confirmWipe = true },
                    destructive = true,
                    isLast = true,
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- Status / last action feedback ---
            StatusBlock(state = state)

            Spacer(Modifier.height(40.dp))
        }

        if (state.busy) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }
    }

    if (confirmWipe) {
        AlertDialog(
            onDismissRequest = { confirmWipe = false },
            title = { Text(stringResource(R.string.settings_wipe_confirm_title)) },
            text = { Text(stringResource(R.string.settings_wipe_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmWipe = false
                    vm.wipeAllData()
                }) {
                    Text(
                        text = stringResource(R.string.settings_wipe_confirm_yes),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmWipe = false }) {
                    Text(stringResource(R.string.settings_wipe_confirm_no))
                }
            },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Column { content() }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    isLast: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (destructive) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.padding(horizontal = 8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (destructive) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    if (!isLast) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        )
    }
}

/**
 * Two-field editor for the user's drinking baseline — drinks/day and price
 * per drink. Values persist via [SettingsViewModel.updateBaseline]. Currency
 * symbol comes from the system locale (NumberFormat); we don't ask the user
 * to pick a currency in v1.
 */
@Composable
private fun BaselineEditor(
    drinksPerDay: Int,
    priceCents: Int,
    hoursPerDrink: Int,
    onSave: (drinksPerDay: Int, priceCents: Int, hoursPerDrink: Int) -> Unit,
) {
    // Show whole-unit currency in the field (e.g. "8" for $8 or "250" for ₹250).
    // We store cents under the hood; the user doesn't care about fractional
    // drinks prices.
    var drinksText by remember(drinksPerDay) {
        mutableStateOf(drinksPerDay.toString())
    }
    var priceText by remember(priceCents) {
        mutableStateOf((priceCents / 100).toString())
    }
    // PHASE_2 §9A2 — whole-hour estimate per drink. Default 1 to hedge the
    // payoff in the Stats card; user can tune upward if they want.
    var hoursText by remember(hoursPerDrink) {
        mutableStateOf(hoursPerDrink.toString())
    }

    val localeSymbol = remember {
        NumberFormat.getCurrencyInstance(Locale.getDefault()).currency?.symbol ?: "$"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_baseline_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = drinksText,
            onValueChange = { new -> drinksText = new.filter { it.isDigit() }.take(2) },
            label = { Text(stringResource(R.string.settings_baseline_drinks_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = priceText,
            onValueChange = { new -> priceText = new.filter { it.isDigit() }.take(6) },
            label = { Text(stringResource(R.string.settings_baseline_price_label)) },
            prefix = { Text(localeSymbol) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            value = hoursText,
            onValueChange = { new -> hoursText = new.filter { it.isDigit() }.take(2) },
            label = { Text(stringResource(R.string.settings_baseline_hours_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = stringResource(R.string.settings_baseline_hours_help),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Button(
            onClick = {
                val d = drinksText.toIntOrNull()?.coerceIn(1, 30) ?: 1
                val p = (priceText.toIntOrNull() ?: 0).coerceAtLeast(0) * 100
                val h = (hoursText.toIntOrNull() ?: 1).coerceIn(0, 24)
                onSave(d, p, h)
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.settings_baseline_save))
        }
    }
}

/**
 * Toggle row for the optional evening mini-review (PHASE_2 §9A5). Off by
 * default; flipping it on surfaces an entry button on Home until the user
 * logs (or the day rolls over).
 */
@Composable
private fun EveningReviewToggleRow(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_evening_review_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.settings_evening_review_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
        )
    }
}

/**
 * Toggle row for the optional visual companion (PHASE_2 §9B1). Off by
 * default. Flipping it on makes an ambient motif appear above the streak
 * on Home — stage grows with streak + check-in activity; never regresses.
 */
@Composable
private fun CompanionToggleRow(
    enabled: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_companion_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.settings_companion_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = onChange,
        )
    }
}

/**
 * Inline confirmation of the most recent action. No toasts — we want the
 * feedback to be visible, not ephemeral, so the user has a moment to read
 * what happened. Dismissable.
 */
@Composable
private fun StatusBlock(state: SettingsUiState) {
    val message = when (state.lastAction) {
        LastAction.Idle, LastAction.Exporting, LastAction.Importing, LastAction.Wiping,
        LastAction.Verifying -> state.message
        LastAction.Exported -> state.message
            ?: stringResource(R.string.settings_status_exported, state.exportedByteCount ?: 0)
        LastAction.Imported -> state.message
            ?: state.lastImportReport?.let { r ->
                stringResource(R.string.settings_status_imported, r.totalRows)
            }
        LastAction.Wiped -> state.message ?: stringResource(R.string.settings_status_wiped)
        LastAction.Verified -> state.message ?: state.verifyReport?.let { r ->
            if (r.matched) stringResource(R.string.settings_verify_ok, r.total)
            else stringResource(R.string.settings_verify_mismatch, r.total - r.roundTripped)
        }
    } ?: return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}
