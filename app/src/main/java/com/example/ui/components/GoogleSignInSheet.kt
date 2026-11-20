package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalCreovaStrings
import com.example.ui.theme.LocalCreovaGlass
import com.example.ui.theme.RosePaid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleSignInSheet(
    isLoading: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSignInClick: () -> Unit
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = glass.glassElevated,
        contentColor = glass.textPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                glass.violetAccent.copy(alpha = 0.35f),
                                glass.cyanAccent.copy(alpha = 0.35f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AccountCircle,
                    contentDescription = null,
                    tint = glass.cyanAccent,
                    modifier = Modifier.size(34.dp)
                )
            }
            Text(
                text = strings.signInRequiredTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = glass.textPrimary
            )
            Text(
                text = strings.signInRequiredBody,
                style = MaterialTheme.typography.bodyMedium,
                color = glass.textSecondary
            )

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = RosePaid
                )
            }

            Button(
                onClick = onSignInClick,
                enabled = !isLoading,
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = glass.violetAccent,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("google_signin_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = androidx.compose.ui.graphics.Color.White
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = strings.signInWithGoogle,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }

            TextButton(onClick = onDismiss) {
                Text(text = strings.cancel, color = glass.textSecondary)
            }
        }
    }
}

@Composable
fun GoogleSignInActionButton(
    isLoading: Boolean,
    onSignInClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val glass = LocalCreovaGlass.current
    val strings = LocalCreovaStrings.current
    Button(
        onClick = onSignInClick,
        enabled = !isLoading,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = glass.violetAccent,
            contentColor = androidx.compose.ui.graphics.Color.White
        ),
        modifier = modifier
            .height(50.dp)
            .testTag("google_signin_button")
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
                color = androidx.compose.ui.graphics.Color.White
            )
        } else {
            Text(
                text = strings.signInWithGoogle,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
