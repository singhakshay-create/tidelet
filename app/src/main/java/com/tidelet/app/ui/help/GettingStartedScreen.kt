package com.tidelet.app.ui.help

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tidelet.app.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GettingStartedScreen(
    onBack: () -> Unit,
    onOpenResources: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.getting_started_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            GuideSection(R.string.getting_started_s1_title, R.string.getting_started_s1_body)
            GuideSection(R.string.getting_started_s2_title, R.string.getting_started_s2_body)
            GuideSection(R.string.getting_started_s3_title, R.string.getting_started_s3_body)
            GuideSection(R.string.getting_started_s4_title, R.string.getting_started_s4_body)
            GuideSection(R.string.getting_started_s5_title, R.string.getting_started_s5_body)
            GuideSection(R.string.getting_started_s6_title, R.string.getting_started_s6_body)
            GuideSection(R.string.getting_started_s7_title, R.string.getting_started_s7_body)

            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onOpenResources) {
                Text(stringResource(R.string.settings_resources))
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun GuideSection(titleRes: Int, bodyRes: Int) {
    Spacer(Modifier.height(16.dp))
    Text(
        text = stringResource(titleRes),
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        text = stringResource(bodyRes),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
