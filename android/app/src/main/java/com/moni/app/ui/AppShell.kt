package com.moni.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.ui.theme.MoniColors

private enum class Tab(val label: String, val icon: Int) {
    Home("Inicio", LucideR.drawable.lucide_ic_house),
    Transactions("Movimientos", LucideR.drawable.lucide_ic_arrow_left_right),
    Accounts("Cuentas", LucideR.drawable.lucide_ic_wallet),
    Profile("Perfil", LucideR.drawable.lucide_ic_user),
}

private val BarHeight = 64.dp
private val BarGap = 16.dp

/**
 * Signed-in shell: the active tab plus a floating bottom bar (pill + round "+").
 * Owns the add and review sheets so any tab can open them.
 */
@Composable
fun AppShell(state: UiState, vm: MoniViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showReview by rememberSaveable { mutableStateOf(false) }
    // Closes the review sheet once the last pending item is resolved.
    LaunchedEffect(state.pending.isEmpty()) { if (state.pending.isEmpty()) showReview = false }
    BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }

    // Tabs lay out above the floating bar: bar + gaps + system navigation inset.
    val contentPadding = PaddingValues(
        bottom = BarHeight + BarGap * 2 + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )
    val openReview = { vm.clearError(); showReview = true }

    Box(Modifier.fillMaxSize().background(MoniColors.bg)) {
        AnimatedContent(
            targetState = tab,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            transitionSpec = {
                (fadeIn(tween(220, delayMillis = 90)) + scaleIn(tween(220, delayMillis = 90), initialScale = 0.98f)) togetherWith
                    fadeOut(tween(90))
            },
            label = "tab",
        ) { current ->
            when (current) {
                Tab.Home -> DashboardScreen(state, contentPadding, onSeeAll = { tab = Tab.Transactions }, onOpenReview = openReview)
                Tab.Transactions -> TransactionsScreen(state, contentPadding, onDelete = vm::delete, onOpenReview = openReview)
                Tab.Accounts -> AccountsScreen(state.transactions, contentPadding, onAccountsChanged = vm::refresh)
                Tab.Profile -> ProfileScreen(state.email, contentPadding, onSignOut = vm::signOut, onCategoriesChanged = vm::refresh)
            }
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = BarGap),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FloatingTabBar(tab, { tab = it }, Modifier.weight(1f))
            AddButton { vm.clearError(); showAdd = true }
        }
    }

    if (showAdd) {
        AddTransactionSheet(
            state = state,
            onAdd = vm::add,
            onDismiss = { showAdd = false; vm.clearError() },
        )
    }
    if (showReview) {
        ReviewSheet(
            state = state,
            onConfirm = vm::confirm,
            onIgnore = vm::ignore,
            onDismiss = { showReview = false; vm.clearError() },
        )
    }
}

@Composable
private fun FloatingTabBar(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(28.dp)
    BoxWithConstraints(
        modifier
            .height(BarHeight)
            .shadow(16.dp, shape, ambientColor = Color(0x401F1E22), spotColor = Color(0x401F1E22))
            .clip(shape)
            .background(MoniColors.card)
            .padding(6.dp)
            .selectableGroup(),
    ) {
        val tabWidth = maxWidth / Tab.entries.size
        val x by animateDpAsState(tabWidth * selected.ordinal, moniSpring(), label = "tabIndicator")
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .width(tabWidth)
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(MoniColors.acc),
        )
        Row(Modifier.fillMaxSize()) {
            Tab.entries.forEach { t ->
                val isSelected = t == selected
                val color by animateColorAsState(if (isSelected) MoniColors.accFg else MoniColors.mut, label = "tabColor")
                Column(
                    Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .clip(RoundedCornerShape(22.dp))
                        .selectable(selected = isSelected, role = Role.Tab) { onSelect(t) },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LucideIcon(t.icon, null, tint = color, size = 20)
                    Text(t.label, color = color, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.sp), maxLines = 1, softWrap = false)
                }
            }
        }
    }
}

@Composable
private fun AddButton(onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            .size(56.dp)
            .pressScale(source)
            .shadow(14.dp, CircleShape, ambientColor = MoniColors.acc.copy(alpha = 0.8f), spotColor = MoniColors.acc.copy(alpha = 0.8f))
            .clip(CircleShape)
            .background(MoniColors.acc)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Añadir movimiento" },
        contentAlignment = Alignment.Center,
    ) { LucideIcon(LucideR.drawable.lucide_ic_plus, null, tint = MoniColors.accFg, size = 24) }
}
