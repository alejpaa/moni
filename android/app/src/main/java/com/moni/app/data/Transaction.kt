package com.moni.app.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors public.transactions (supabase/migrations). The database schema is
// the shared contract between web and Android; see docs/adr/0002.
@Serializable
data class Transaction(
    val id: String,
    @SerialName("user_id") val userId: String,
    val type: String,
    val amount: Double,
    val category: String,
    @SerialName("occurred_at") val occurredAt: String,
    val note: String? = null,
) {
    val isIncome get() = type == "income"
}

@Serializable
data class NewTransaction(
    @SerialName("user_id") val userId: String,
    val type: String,
    val amount: Double,
    val category: String,
    val note: String? = null,
)

data class Summary(val income: Double, val expense: Double) {
    val balance get() = income - expense
}

fun List<Transaction>.summary() = Summary(
    income = filter { it.isIncome }.sumOf { it.amount },
    expense = filterNot { it.isIncome }.sumOf { it.amount },
)
