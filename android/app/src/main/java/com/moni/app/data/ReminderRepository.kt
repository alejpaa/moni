package com.moni.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
data class Reminder(
    val id: String,
    @SerialName("workspace_id") val workspaceId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val kind: String,
    val title: String,
    val amount: Double? = null,
    @SerialName("due_date") val dueDate: String,
    val frequency: String? = null,
    @SerialName("remind_days_before") val remindDaysBefore: Int = 2,
    @SerialName("account_id") val accountId: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    val note: String? = null,
    @SerialName("is_done") val isDone: Boolean = false,
)

/** workspace_id and user_id are omitted: the DB defaults them to the caller's workspace. */
@Serializable
data class NewReminder(
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

    suspend fun listActive(): List<Reminder> =
        table.select(
            Columns.list(
                "id", "workspace_id", "user_id", "kind", "title", "amount",
                "due_date", "frequency", "remind_days_before", "account_id", "category_id", "note", "is_done",
            ),
        ) {
            filter {
                eq("is_done", false)
                isIn("frequency", listOf("weekly", "monthly", "yearly"))
            }
            order("due_date", Order.ASCENDING)
        }.decodeList()

    suspend fun create(input: NewReminder): Reminder =
        table.insert(input.copy(amount = round2(input.amount))) { select() }.decodeSingle()

    suspend fun updateDueDate(id: String, dueDate: String) {
        table.update(buildJsonObject { put("due_date", dueDate) }) { filter { eq("id", id) } }
    }

    suspend fun markDone(id: String) {
        table.update(buildJsonObject { put("is_done", true) }) { filter { eq("id", id) } }
    }
}
