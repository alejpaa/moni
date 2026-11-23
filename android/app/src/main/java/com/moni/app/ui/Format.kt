package com.moni.app.ui

import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val esLocale = Locale.forLanguageTag("es")

/** Spanish number format with exactly two decimals ("1.234,50"). PEN only. */
internal val money: NumberFormat = NumberFormat.getNumberInstance(esLocale).apply {
    minimumFractionDigits = 2
    maximumFractionDigits = 2
}

private val dayMonth = DateTimeFormatter.ofPattern("d MMM", esLocale)
private val dayMonthYear = DateTimeFormatter.ofPattern("d MMM yyyy", esLocale)

/** "2026-11-22T10:00:00+00:00" -> "22 nov" (adds the year when not the current one). */
internal fun formatDate(iso: String): String {
    val date = runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }
        .recoverCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() }
        .recoverCatching { LocalDate.parse(iso.take(10)) }
        .getOrNull() ?: return iso.take(10)
    val f = if (date.year == LocalDate.now().year) dayMonth else dayMonthYear
    return f.format(date).replace(".", "")
}
