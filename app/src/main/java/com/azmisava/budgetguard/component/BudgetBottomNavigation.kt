package com.azmisava.budgetguard.component

import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.azmisava.budgetguard.BackgroundSoftLilac
import com.azmisava.budgetguard.PrimaryLavender
import com.azmisava.budgetguard.TextMuted
import com.azmisava.budgetguard.model.BudgetTab

@Composable
fun BudgetBottomNavigation(
    selectedTab: BudgetTab,
    onTabSelected: (BudgetTab) -> Unit
) {
    // Bottom navigation digunakan untuk berpindah halaman utama aplikasi
    NavigationBar(
        containerColor = Color.White
    ) {
        BudgetTab.values().forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = {
                    // Mengubah tab aktif sesuai menu yang diklik
                    onTabSelected(tab)
                },
                icon = {
                    Text(
                        text = tab.icon,
                        fontSize = 18.sp
                    )
                },
                label = {
                    Text(
                        text = tab.title,
                        fontSize = 10.sp,
                        maxLines = 1
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryLavender,
                    selectedTextColor = PrimaryLavender,
                    unselectedIconColor = TextMuted,
                    unselectedTextColor = TextMuted,
                    indicatorColor = BackgroundSoftLilac
                )
            )
        }
    }
}