package com.moni.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Income = Color(0xFF047857)
val Expense = Color(0xFFB91C1C)
val IncomeDark = Color(0xFF34D399)
val ExpenseDark = Color(0xFFF87171)

private val Light = lightColorScheme(
    primary = Color(0xFF047857),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF34D399),
    background = Color(0xFF0B1120),
    surface = Color(0xFF111827),
)

@Composable
fun MoniTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}

@Composable
fun incomeColor() = if (isSystemInDarkTheme()) IncomeDark else Income

@Composable
fun expenseColor() = if (isSystemInDarkTheme()) ExpenseDark else Expense
