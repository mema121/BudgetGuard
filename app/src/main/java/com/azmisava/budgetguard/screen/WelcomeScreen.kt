package com.azmisava.budgetguard.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.azmisava.budgetguard.BackgroundSoftLilac
import com.azmisava.budgetguard.PrimaryLavender
import com.azmisava.budgetguard.TextMain
import com.azmisava.budgetguard.TextMuted

@Composable
fun WelcomeScreen(
    onStart: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoftLilac)
            .padding(horizontal = 28.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .background(
                        color = Color(0xFFF0E8FF),
                        shape = RoundedCornerShape(28.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryLavender
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "BudgetGuard",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = TextMain
            )

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Kontrol budget pribadi dengan aman dan terarah.",
                fontSize = 16.sp,
                color = TextMuted,
                textAlign = TextAlign.Center,
                lineHeight = 23.sp
            )
        }

        Button(
            onClick = {
                onStart()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(56.dp)
                .padding(bottom = 24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryLavender
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "Mulai",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
