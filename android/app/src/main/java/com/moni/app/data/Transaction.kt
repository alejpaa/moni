package com.moni.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors public.transactions / categories / accounts (supabase/migrations,
// packages/shared/src/database.types.ts). The schema is the contract between
// web and Android; see docs/adr/0007.
@Serializable
data class Transaction(
    val id: String,
    @SerialName("workspace_id") val workspaceId: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val type: String,
    val amount: Double,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("account_id") val accountId: String? = null,
    val merchant: String? = null,
    val source: String = "manual",
    val status: String = "confirmed",
    @SerialName("occurred_at") val occurredAt: String,
    val note: String? = null,
    @SerialName("reminder_id") val reminderId: String? = null,
) {
    val isIncome get() = type == "income"
}

/** workspace_id and user_id are omitted: the DB defaults them to the caller's workspace. */
@Serializable
data class NewTransaction(
    val type: String,
    val amount: Double,
    @SerialName("category_id") val categoryId: String,
    @SerialName("account_id") val accountId: String?,
    val note: String? = null,
    val source: String = "manual",
    val status: String = "confirmed",
    @SerialName("reminder_id") val reminderId: String? = null,
)

@Serializable
data class Category(
    val id: String,
    val name: String,
    val kind: String,
    val icon: String? = null,
    val archived: Boolean = false,
)

@Serializable
data class Account(
    val id: String,
    val name: String,
    val type: String,
    @SerialName("opening_balance") val openingBalance: Double = 0.0,
    val institution: String? = null,
    val last4: String? = null,
    val color: String? = null,
    @SerialName("credit_limit") val creditLimit: Double? = null,
    @SerialName("closing_day") val closingDay: Int? = null,
    @SerialName("due_day") val dueDay: Int? = null,
    val archived: Boolean = false,
)

data class Summary(val income: Double, val expense: Double) {
    val balance get() = income - expense
}

/** Callers pass confirmed transactions only; pending/ignored never count. */
fun List<Transaction>.summary(): Summary {
    val confirmed = filter { it.status == "confirmed" }
    return Summary(
        income = confirmed.filter { it.isIncome }.sumOf { it.amount },
        expense = confirmed.filterNot { it.isIncome }.sumOf { it.amount },
    )
}
