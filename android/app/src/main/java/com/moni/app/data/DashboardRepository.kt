package com.moni.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.time.ZoneId

/** Slim projection of a confirmed transaction for dashboard aggregates. */
@Serializable
data class TxLite(
    val type: String,
    val amount: Double,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("occurred_at") val occurredAt: String? = null,
)

class DashboardRepository {
    private val table get() = supabase.from("transactions")

    /** Confirmed transactions since the first day of the current month (local time). */
    suspend fun currentMonth(): List<TxLite> {
        val start = LocalDate.now().withDayOfMonth(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toString()
        return paged(Columns.list("type", "amount", "category_id", "account_id", "occurred_at")) {
            filter {
                eq("status", "confirmed")
                gte("occurred_at", start)
            }
        }
    }

    private suspend fun paged(
        columns: Columns,
        build: io.github.jan.supabase.postgrest.query.PostgrestRequestBuilder.() -> Unit,
    ): List<TxLite> {
        val out = ArrayList<TxLite>()
        var from = 0L
        val size = 1000L
        while (true) {
            val chunk = table.select(columns) {
                build()
                order("id", io.github.jan.supabase.postgrest.query.Order.ASCENDING)
                range(from, from + size - 1)
            }.decodeList<TxLite>()
            out += chunk
            if (chunk.size < size) break
            from += size
        }
        return out
    }
}
