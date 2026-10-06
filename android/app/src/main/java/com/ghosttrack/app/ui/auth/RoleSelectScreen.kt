package com.ghosttrack.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghosttrack.app.ui.components.GhostTrackButton
import com.ghosttrack.app.ui.components.GhostTrackTextField
import com.ghosttrack.app.ui.theme.*

@Composable
fun RoleSelectScreen(
    onSelectStudent: () -> Unit,
    onSelectDriver: () -> Unit,
    onSelectAdmin: () -> Unit,
    currentServerUrl: String = "http://10.0.2.2:4000/",
    onUpdateServerUrl: (String) -> Unit = {}
) {
    var showServerDialog by remember { mutableStateOf(false) }
    var inputUrl by remember(currentServerUrl) { mutableStateOf(currentServerUrl) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemGroupedBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // App Icon
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(SurfaceWhite)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🚌", fontSize = 34.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "GIET Bus Tracker",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                ),
                color = LabelPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Select your role to continue",
                style = MaterialTheme.typography.bodyMedium,
                color = SystemGray
            )

            Spacer(modifier = Modifier.height(36.dp))

            // White Card with Role Buttons
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    GhostTrackButton(
                        text = "Student",
                        onClick = onSelectStudent,
                        containerColor = LabelPrimary,
                        contentColor = SurfaceWhite
                    )

                    GhostTrackButton(
                        text = "Driver",
                        onClick = onSelectDriver,
                        containerColor = AppleOrange,
                        contentColor = SurfaceWhite
                    )

                    GhostTrackButton(
                        text = "Admin",
                        onClick = onSelectAdmin,
                        containerColor = AppleBlue,
                        contentColor = SurfaceWhite
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Server Config Pill Button
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { showServerDialog = true },
                color = SurfaceWhite,
                shadowElevation = 1.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(text = "⚙️", fontSize = 13.sp)
                    Text(
                        text = "Backend: $currentServerUrl",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        ),
                        color = SystemGray
                    )
                }
            }
        }
    }

    if (showServerDialog) {
        AlertDialog(
            onDismissRequest = { showServerDialog = false },
            title = {
                Text(
                    text = "Backend Connection",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Connect to the same backend running your web app & MongoDB:",
                        style = MaterialTheme.typography.bodySmall,
                        color = SystemGray
                    )

                    GhostTrackTextField(
                        value = inputUrl,
                        onValueChange = { inputUrl = it },
                        label = "Server URL",
                        placeholder = "http://10.0.2.2:4000/"
                    )

                    Text(
                        text = "Quick Presets:",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = LabelPrimary
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedButton(
                            onClick = { inputUrl = "http://10.0.2.2:4000/" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(text = "Emulator", fontSize = 11.sp)
                        }

                        OutlinedButton(
                            onClick = { inputUrl = "http://10.62.218.137:4000/" },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(text = "Wi-Fi PC", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmed = inputUrl.trim()
                        val finalUrl = if (trimmed.endsWith("/")) trimmed else "$trimmed/"
                        onUpdateServerUrl(finalUrl)
                        showServerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AppleBlue)
                ) {
                    Text("Save & Connect")
                }
            },
            dismissButton = {
                TextButton(onClick = { showServerDialog = false }) {
                    Text("Cancel", color = SystemGray)
                }
            }
        )
    }
}
