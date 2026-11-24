package com.moni.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Reminder(
    val id: String,
    val kind: String,
    val title: String,
    val amount: Double? = null,
    @SerialName("due_date") val dueDate: String,
    val frequency: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("is_done") val isDone: Boolean = false,
)

@Serializable
data class NewReminder(
    @SerialName("workspace_id") val workspaceId: String,
    val kind: String,
    val title: String,
    val amount: Double,
    @SerialName("due_date") val dueDate: String,
    val frequency: String,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val note: String? = null,
)

class ReminderRepository {
    private val table get() = supabase.from("reminders")
    private val columns = Columns.list("id", "kind", "title", "amount", "due_date", "frequency", "category_id", "is_done")

    suspend fun listActive(workspaceId: String): List<Reminder> =
        table.select(columns) {
            filter {
                eq("workspace_id", workspaceId)
                eq("is_done", false)
                isIn("frequency", listOf("weekly", "monthly", "yearly"))
            }
            order("due_date", Order.ASCENDING)
        }.decodeList()

    suspend fun create(input: NewReminder): Reminder =
        table.insert(input.copy(amount = round2(input.amount))) { select(columns) }.decodeSingle()

    suspend fun delete(workspaceId: String, id: String) {
        table.delete {
            filter {
                eq("id", id)
                eq("workspace_id", workspaceId)
            }
        }
    }
}
