package com.moni.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TransactionRepository {
    private val table get() = supabase.from("transactions")

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id
    fun currentEmail(): String? = supabase.auth.currentUserOrNull()?.email

    suspend fun signIn(email: String, password: String) =
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }

    suspend fun signUp(email: String, password: String) {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() = supabase.auth.signOut()

    suspend fun list(): List<Transaction> = byStatus("confirmed", 100)

    suspend fun listPending(): List<Transaction> = byStatus("pending", 50)

    private suspend fun byStatus(status: String, max: Long): List<Transaction> =
        table.select {
            filter { eq("status", status) }
            order("occurred_at", Order.DESCENDING)
            limit(max)
        }.decodeList()

    suspend fun add(
        type: String,
        amount: Double,
        categoryId: String,
        accountId: String?,
        note: String?,
        reminderId: String? = null,
    ): Transaction =
        table.insert(NewTransaction(type, round2(amount), categoryId, accountId, note, reminderId = reminderId)) {
            select()
        }.decodeSingle()

    /** Confirms or ignores a pending row, optionally reassigning category/account. */
    suspend fun review(id: String, status: String, categoryId: String? = null, accountId: String? = null): Transaction {
        // Built by hand so unchanged fields are never sent (a null would clear the column).
        val patch = buildJsonObject {
            put("status", status)
            categoryId?.let { put("category_id", it) }
            accountId?.let { put("account_id", it) }
        }
        return table.update(patch) {
            filter { eq("id", id) }
            select()
        }.decodeSingle()
    }

    suspend fun delete(id: String) {
        table.delete { filter { eq("id", id) } }
    }
}
