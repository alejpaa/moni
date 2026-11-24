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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as LucideR
import com.moni.app.ui.theme.MoniColors
import kotlinx.coroutines.delay

private enum class Tab(val label: String, val icon: Int) {
    Home("Inicio", LucideR.drawable.lucide_ic_house),
    Transactions("Movimientos", LucideR.drawable.lucide_ic_arrow_left_right),
    Accounts("Cuentas", LucideR.drawable.lucide_ic_wallet),
    Profile("Perfil", LucideR.drawable.lucide_ic_user),
}

private val BarHeight = 64.dp
private val BarGap = 16.dp
private val FabSize = 56.dp
private val FabGap = 12.dp

/**
 * Signed-in shell: the active tab plus a floating bottom bar (full-width pill with a "+" FAB stacked above it).
 * Owns the add and review sheets so any tab can open them.
 */
@Composable
fun AppShell(state: UiState, vm: MoniViewModel, profileVm: ProfileViewModel) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    var showAdd by rememberSaveable { mutableStateOf(false) }
    var showReview by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val animScale = remember(context) { systemAnimatorScale(context) }

    // Closes the review sheet once the last pending item is resolved or if role becomes viewer.
    LaunchedEffect(state.pending.isEmpty(), state.isViewer) {
        if (state.pending.isEmpty() || state.isViewer) showReview = false
        if (state.isViewer) showAdd = false
    }
    BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }

    // Tabs lay out above the floating bar and FAB: bar + FAB + gaps + system navigation inset.
    val contentPadding = PaddingValues(
        bottom = BarHeight + BarGap * 2 + FabGap + FabSize + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )
    val openReview = {
        if (!state.isViewer) {
            vm.clearError()
            showReview = true
        }
    }

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
            val animateEntrance = remember(current) { animScale > 0f && vm.shouldAnimateEntrance(current.name) }
            LaunchedEffect(current) {
                val wait = scaledDelayMs(850, animScale)
                if (wait > 0) delay(wait)
                vm.markTabEntered(current.name)
            }
            DisposableEffect(current) {
                onDispose { vm.markTabEntered(current.name) }
            }
            CompositionLocalProvider(LocalAnimateEntrance provides animateEntrance) {
                when (current) {
                    Tab.Home -> DashboardScreen(
                        state = state,
                        contentPadding = contentPadding,
                        shouldAnimateHero = vm::shouldAnimateHero,
                        onHeroAnimated = vm::markHeroAnimated,
                        shouldAnimateBars = vm::shouldAnimateDashboardBars,
                        onBarsAnimated = vm::markDashboardBarsAnimated,
                        onSeeAll = { tab = Tab.Transactions },
                        onOpenReview = openReview,
                        onRetry = vm::refresh,
                        onRetrySession = vm::retrySessionOrRefresh,
                    )
                    Tab.Transactions -> TransactionsScreen(
                        state = state,
                        contentPadding = contentPadding,
                        tracker = vm.rowTracker,
                        onDelete = vm::delete,
                        onOpenReview = openReview,
                        onLoadMore = vm::loadMoreTransactions,
                        onRetry = vm::refresh,
                    )
                    Tab.Accounts -> AccountsScreen(
                        state = state,
                        contentPadding = contentPadding,
                        onClearError = vm::clearError,
                        onSave = vm::saveAccount,
                        onSetArchived = vm::setAccountArchived,
                        onRetry = vm::refresh,
                    )
                    Tab.Profile -> ProfileScreen(
                        state = state,
                        vm = profileVm,
                        contentPadding = contentPadding,
                        onSignOut = vm::signOut,
                        onClearSheetError = vm::clearError,
                        onCreateCategory = vm::createCategory,
                        onUpdateCategory = vm::updateCategory,
                        onSetCategoryArchived = vm::setCategoryArchived,
                    )
                }
            }
        }

        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, bottom = BarGap),
            verticalArrangement = Arrangement.spacedBy(FabGap),
            horizontalAlignment = Alignment.End,
        ) {
            if (!state.isViewer) {
                AddButton { vm.clearError(); showAdd = true }
            }
            FloatingTabBar(tab, { tab = it }, Modifier.fillMaxWidth())
        }
    }

    if (showAdd && !state.isViewer) {
        AddTransactionSheet(
            state = state,
            onAdd = vm::add,
            onRetryCategories = vm::retryCategories,
            onDismiss = { showAdd = false; vm.clearError() },
        )
    }
    if (showReview && !state.isViewer) {
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
        val x = animateDpAsState(tabWidth * selected.ordinal, moniSpring(), label = "tabIndicator")
        Box(
            Modifier
                .offset { IntOffset(x.value.roundToPx(), 0) }
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
                    Text(t.label, color = color, style = MaterialTheme.typography.labelSmall, maxLines = 1, softWrap = false)
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
            .size(FabSize)
            .pressScale(source)
            .shadow(14.dp, CircleShape, ambientColor = MoniColors.acc.copy(alpha = 0.8f), spotColor = MoniColors.acc.copy(alpha = 0.8f))
            .clip(CircleShape)
            .background(MoniColors.acc)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Añadir movimiento" },
        contentAlignment = Alignment.Center,
    ) { LucideIcon(LucideR.drawable.lucide_ic_plus, null, tint = MoniColors.accFg, size = 24) }
}
