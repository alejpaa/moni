package com.moni.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate

/** Slim projection of a confirmed transaction for dashboard aggregates. */
@Serializable
data class TxLite(
    val type: String,
    val amount: Double,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("occurred_at") val occurredAt: String? = null,
)

class DashboardRepository {
    private val table get() = supabase.from("transactions")

    /** Confirmed transactions in the current America/Lima calendar month. */
    suspend fun currentMonth(workspaceId: String): List<TxLite> {
        val firstOfMonth = LocalDate.now(LIMA_ZONE).withDayOfMonth(1)
        val start = firstOfMonth.atStartOfDay(LIMA_ZONE).toInstant().toString()
        val end = firstOfMonth.plusMonths(1).atStartOfDay(LIMA_ZONE).toInstant().toString()
        val columns = Columns.list("type", "amount", "category_id", "occurred_at")
        val out = ArrayList<TxLite>()
        var from = 0L
        val size = 1000L
        while (true) {
            val chunk = table.select(columns) {
                filter {
                    eq("workspace_id", workspaceId)
                    eq("status", "confirmed")
                    gte("occurred_at", start)
                    lt("occurred_at", end)
                }
                order("id", Order.ASCENDING)
                range(from, from + size - 1)
            }.decodeList<TxLite>()
            out += chunk
            if (chunk.size < size) break
            from += size
        }
        return out
    }
}
