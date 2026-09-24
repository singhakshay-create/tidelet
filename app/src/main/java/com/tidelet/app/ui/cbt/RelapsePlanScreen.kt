package com.tidelet.app.ui.cbt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R

/**
 * Relapse Prevention Plan screen.
 *
 * Three long-form prompts from Marlatt's classic RP framework:
 *   1. high-risk situations       — places/people/feelings that have led to
 *                                   drinking before
 *   2. early warning signs        — how the craving shows up in the body/mind
 *                                   before the decision point
 *   3. coping plan                — what the user will do instead
 *
 * Not a wizard, not scored, not shared. The user writes this calmly (usually
 * from Settings, but also discoverable from the Reasons screen during a
 * craving). On save we write the single row back; the VM confirms visibly
 * without navigating — the user should feel the write succeeded without
 * being punted elsewhere.
 *
 * No FilterChips, no AI suggestions, no templates. This is a blank page on
 * purpose — the clinical value is in the user's own words.
 */
@Composable
fun RelapsePlanScreen(
    onBack: () -> Unit,
    vm: RelapsePlanViewModel = viewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 24.dp),
        ) {
            // Header row — back button + title
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.relapse_plan_back),
                    )
                }
                Spacer(Modifier.padding(4.dp))
                Text(
                    text = stringResource(R.string.relapse_plan_title),
                    style = MaterialTheme.typography.headlineSmall,
                )
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.relapse_plan_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))

            // --- 1. High-risk situations ---
            SectionHeader(
                titleRes = R.string.relapse_plan_high_risk_title,
                helperRes = R.string.relapse_plan_high_risk_help,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.highRiskSituations,
                onValueChange = vm::setHighRiskSituations,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = {
                    Text(stringResource(R.string.relapse_plan_high_risk_hint))
                },
            )

            Spacer(Modifier.height(20.dp))

            // --- 2. Early warning signs ---
            SectionHeader(
                titleRes = R.string.relapse_plan_warnings_title,
                helperRes = R.string.relapse_plan_warnings_help,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.earlyWarningSigns,
                onValueChange = vm::setEarlyWarningSigns,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = {
                    Text(stringResource(R.string.relapse_plan_warnings_hint))
                },
            )

            Spacer(Modifier.height(20.dp))

            // --- 3. Coping plan ---
            SectionHeader(
                titleRes = R.string.relapse_plan_coping_title,
                helperRes = R.string.relapse_plan_coping_help,
            )
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = state.copingPlan,
                onValueChange = vm::setCopingPlan,
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                placeholder = {
                    Text(stringResource(R.string.relapse_plan_coping_hint))
                },
            )

            Spacer(Modifier.height(28.dp))

            Button(
                onClick = vm::save,
                modifier = Modifier.fillMaxWidth(),
                // Always enabled — an all-empty plan is technically valid and
                // saving it shouldn't require us to second-guess the user.
            ) {
                Text(
                    text = if (state.hasExistingPlan) {
                        stringResource(R.string.relapse_plan_save_update)
                    } else {
                        stringResource(R.string.relapse_plan_save_create)
                    },
                )
            }

            if (state.justSaved) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.relapse_plan_saved),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun SectionHeader(titleRes: Int, helperRes: Int) {
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleMedium,
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = stringResource(helperRes),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
