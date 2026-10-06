package com.ghosttrack.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ghosttrack.app.ui.components.GhostTrackButton
import com.ghosttrack.app.ui.theme.*

@Composable
fun RoleSelectScreen(
    onSelectStudent: () -> Unit,
    onSelectDriver: () -> Unit,
    onSelectAdmin: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SystemGroupedBackground)
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
        }
    }
}
