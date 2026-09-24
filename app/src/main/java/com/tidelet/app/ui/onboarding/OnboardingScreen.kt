package com.tidelet.app.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tidelet.app.R
import com.tidelet.app.util.todayLocal
import java.time.LocalDate

/**
 * The three-step onboarding:
 *   1. Welcome.
 *   2. Pick the sobriety start date.
 *   3. Finish + safety notice.
 *
 * The whole thing is state-hoisted: a single [step] holds the current screen,
 * and a single [pickedDate] holds the chosen start date. No nav graph needed
 * for something this small.
 */
private enum class OnboardingStep { Welcome, StartDate, Done }

@Composable
fun OnboardingFlow(vm: OnboardingViewModel = viewModel()) {
    var step by rememberSaveable { mutableStateOf(OnboardingStep.Welcome) }
    var pickedDate by remember { mutableStateOf(todayLocal()) }

    when (step) {
        OnboardingStep.Welcome -> WelcomePage(onContinue = { step = OnboardingStep.StartDate })
        OnboardingStep.StartDate -> StartDatePage(
            onPick = { date ->
                pickedDate = date
                step = OnboardingStep.Done
            }
        )
        OnboardingStep.Done -> DonePage(onFinish = { vm.finishOnboarding(pickedDate) })
    }
}

// ---- Step 1: Welcome ----

@Composable
private fun WelcomePage(onContinue: () -> Unit) {
    PageScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                stringResource(R.string.app_name),
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Text(
                stringResource(R.string.onboarding_welcome_title),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.onboarding_welcome_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_welcome_continue"),
            ) { Text(stringResource(R.string.onboarding_continue)) }
        }
    }
}

// ---- Step 2: Start date ----

@Composable
private fun StartDatePage(onPick: (LocalDate) -> Unit) {
    PageScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.onboarding_start_date_title),
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.onboarding_start_date_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))

            Button(
                onClick = { onPick(todayLocal()) },
                modifier = Modifier.fillMaxWidth().testTag("onboarding_start_date_today"),
            ) { Text(stringResource(R.string.onboarding_start_date_today)) }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = { onPick(todayLocal().minusDays(1)) },
                modifier = Modifier.fillMaxWidth().testTag("onboarding_start_date_yesterday"),
            ) { Text(stringResource(R.string.onboarding_start_date_yesterday)) }

            Spacer(Modifier.height(12.dp))

            // A full date-picker dialog is a phase-2 polish task. For v1 we nudge earlier
            // starters to "a week ago" as a sensible default; they can refine in Settings.
            OutlinedButton(
                onClick = { onPick(todayLocal().minusDays(7)) },
                modifier = Modifier.fillMaxWidth().testTag("onboarding_start_date_earlier"),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) { Text(stringResource(R.string.onboarding_start_date_earlier)) }
        }
    }
}

// ---- Step 3: Done ----

@Composable
private fun DonePage(onFinish: () -> Unit) {
    PageScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 48.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                stringResource(R.string.onboarding_done_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                stringResource(R.string.onboarding_done_body),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = onFinish,
                modifier = Modifier.fillMaxWidth().testTag("onboarding_finish"),
            ) { Text(stringResource(R.string.onboarding_finish)) }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.onboarding_guide_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---- Shared page frame ----

@Composable
private fun PageScaffold(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) { content() }
}
