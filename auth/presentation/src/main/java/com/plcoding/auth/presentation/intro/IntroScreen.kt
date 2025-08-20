package com.avi.auth.presentation.intro

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avi.auth.presentation.R
import com.avi.core.presentation.designsystem.LogoIcon
import com.avi.core.presentation.designsystem.Poppins
import com.avi.core.presentation.designsystem.RuniqueTheme
import com.avi.core.presentation.designsystem.components.GradientBackground
import com.avi.core.presentation.designsystem.components.RuniqueActionButton
import com.avi.core.presentation.designsystem.components.RuniqueOutlinedActionButton
import java.time.LocalDateTime

@Composable
fun IntroScreenRoot(
    onSignUpClick: () -> Unit,
    onSignInClick: () -> Unit
) {
    IntroScreen(
        onAction = { action ->
            when(action) {
                IntroAction.OnSignInClick -> onSignInClick()
                IntroAction.OnSignUpClick -> onSignUpClick()
            }
        }
    )
}

@Composable
fun IntroScreen(
    onAction: (IntroAction) -> Unit
) {
    GradientBackground {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            RuniqueLogoVertical()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 48.dp)
        ) {
            Text(
                text = stringResource(id = R.string.welcome_to_fitnesstracker),
                style = Poppins.h1,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(id = R.string.fitnesstracker_description),
                style = Poppins.body1,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(32.dp))
            RuniqueOutlinedActionButton(
                text = stringResource(id = R.string.skip),
                onClick = onSkip,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            RuniqueActionButton(
                text = stringResource(id = R.string.get_started),
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun RuniqueLogoVertical(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.fitnesstracker),
            style = Poppins.h2,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Preview
@Composable
private fun IntroScreenPreview() {
    RuniqueTheme {
        IntroScreen(
            onAction = {}
        )
    }
}