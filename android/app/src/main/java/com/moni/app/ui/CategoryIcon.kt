package com.moni.app.ui

import androidx.annotation.DrawableRes
import com.composables.icons.lucide.R as LucideR
import java.text.Normalizer

private val accents = Regex("\\p{InCombiningDiacriticalMarks}+")

/** Lowercase, accent-free form used for keyword matching. */
fun normalizeCategory(raw: String): String =
    Normalizer.normalize(raw.trim().lowercase(), Normalizer.Form.NFD).replace(accents, "")

// Order matters: first match wins ("gasolina" before the generic transport words).
private val rules: List<Pair<List<String>, Int>> = listOf(
    listOf("gasolina") to LucideR.drawable.lucide_ic_fuel,
    listOf("sueldo", "salario", "nomina", "pago") to LucideR.drawable.lucide_ic_briefcase,
    listOf("comida", "almuerzo", "cena", "restaurante") to LucideR.drawable.lucide_ic_utensils,
    listOf("super", "supermercado", "mercado", "compras") to LucideR.drawable.lucide_ic_shopping_cart,
    listOf("cafe") to LucideR.drawable.lucide_ic_coffee,
    listOf("transporte", "taxi", "bus", "uber") to LucideR.drawable.lucide_ic_bus,
    listOf("casa", "alquiler", "renta", "hogar") to LucideR.drawable.lucide_ic_house,
    listOf("luz", "agua", "internet", "servicios") to LucideR.drawable.lucide_ic_zap,
    listOf("salud", "farmacia", "medico") to LucideR.drawable.lucide_ic_heart_pulse,
    listOf("ocio", "cine", "juegos") to LucideR.drawable.lucide_ic_clapperboard,
    listOf("regalo") to LucideR.drawable.lucide_ic_gift,
    listOf("viaje", "vuelo") to LucideR.drawable.lucide_ic_plane,
    listOf("educacion", "curso", "libros") to LucideR.drawable.lucide_ic_graduation_cap,
    listOf("ahorro", "inversion") to LucideR.drawable.lucide_ic_piggy_bank,
)

/** Free-text category -> Lucide drawable by keyword, with a default per type. */
@DrawableRes
fun categoryIcon(category: String, isIncome: Boolean): Int {
    val text = normalizeCategory(category)
    val words = text.split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
    for ((keys, icon) in rules) {
        if (keys.any { it in words || (it.length > 3 && text.contains(it)) }) return icon
    }
    return if (isIncome) LucideR.drawable.lucide_ic_arrow_down_left else LucideR.drawable.lucide_ic_arrow_up_right
}

private val iconKeys: Map<String, Int> = mapOf(
    "utensils" to LucideR.drawable.lucide_ic_utensils,
    "shopping-cart" to LucideR.drawable.lucide_ic_shopping_cart,
    "shopping-bag" to LucideR.drawable.lucide_ic_shopping_bag,
    "bus" to LucideR.drawable.lucide_ic_bus,
    "house" to LucideR.drawable.lucide_ic_house,
    "zap" to LucideR.drawable.lucide_ic_zap,
    "heart-pulse" to LucideR.drawable.lucide_ic_heart_pulse,
    "clapperboard" to LucideR.drawable.lucide_ic_clapperboard,
    "graduation-cap" to LucideR.drawable.lucide_ic_graduation_cap,
    "plane" to LucideR.drawable.lucide_ic_plane,
    "repeat" to LucideR.drawable.lucide_ic_repeat,
    "arrow-up-right" to LucideR.drawable.lucide_ic_arrow_up_right,
    "arrow-down-left" to LucideR.drawable.lucide_ic_arrow_down_left,
    "briefcase" to LucideR.drawable.lucide_ic_briefcase,
    "gift" to LucideR.drawable.lucide_ic_gift,
    "store" to LucideR.drawable.lucide_ic_store,
    "rotate-ccw" to LucideR.drawable.lucide_ic_rotate_ccw,
    "coffee" to LucideR.drawable.lucide_ic_coffee,
    "fuel" to LucideR.drawable.lucide_ic_fuel,
    "piggy-bank" to LucideR.drawable.lucide_ic_piggy_bank,
    "wallet" to LucideR.drawable.lucide_ic_wallet,
)

/** Icon for a stored category: its Lucide `icon` key, else the keyword mapping by name, else a per-type default. */
@DrawableRes
fun categoryIcon(c: com.moni.app.data.Category?, isIncome: Boolean): Int =
    c?.icon?.let { iconKeys[it.trim().lowercase()] } ?: categoryIcon(c?.name.orEmpty(), isIncome)
