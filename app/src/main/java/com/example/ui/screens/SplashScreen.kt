package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.FocusGold
import com.example.ui.theme.FocusNavy
import com.example.ui.theme.FocusPrimaryBlue
import com.example.ui.theme.FocusRoyalBlue
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onEnterDashboard: () -> Unit
) {
    val alphaAnim = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, animationSpec = tween(700))
        delay(1400)
        onEnterDashboard()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        FocusNavy,
                        Color(0xFF092348),
                        Color(0xFF061833)
                    )
                )
            )
            .testTag("splash_screen_root")
    ) {
        // Decorative Golden & Blue Wave curves at bottom
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .align(Alignment.BottomCenter)
        ) {
            val width = size.width
            val height = size.height

            // Royal blue wave
            val bluePath = Path().apply {
                moveTo(0f, height * 0.45f)
                cubicTo(
                    width * 0.35f, height * 0.35f,
                    width * 0.65f, height * 0.65f,
                    width, height * 0.40f
                )
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(
                path = bluePath,
                color = FocusRoyalBlue.copy(alpha = 0.5f)
            )

            // Golden curve
            val goldPath = Path().apply {
                moveTo(0f, height * 0.55f)
                cubicTo(
                    width * 0.3f, height * 0.50f,
                    width * 0.7f, height * 0.75f,
                    width, height * 0.50f
                )
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(
                path = goldPath,
                color = FocusGold.copy(alpha = 0.85f)
            )
        }

        // Center Content
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp)
                .alpha(alphaAnim.value)
        ) {
            // Focus Classes Logo
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(Color.White.copy(alpha = 0.1f), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.School,
                    contentDescription = "Focus Classes Logo",
                    tint = Color.White,
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Brand Title
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "FOCUS ",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "CLASSES",
                    color = FocusGold,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tagline
            Text(
                text = "Learn  •  Grow  •  Excel",
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.5.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Track Progress. Build Leaders.",
                color = FocusGold,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        // Footer Text and Fast Enter Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 36.dp, start = 24.dp, end = 24.dp)
                .alpha(alphaAnim.value)
        ) {
            Text(
                text = "Building Better Students\nfor a Brighter Future",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onEnterDashboard,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FocusPrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("splash_enter_button")
            ) {
                Text("Enter Dashboard", fontWeight = FontWeight.Bold)
            }
        }
    }
}
