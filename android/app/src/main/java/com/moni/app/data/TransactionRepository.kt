package com.moni.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order

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

    suspend fun list(): List<Transaction> =
        table.select {
            order("occurred_at", Order.DESCENDING)
            limit(100)
        }.decodeList()

    suspend fun add(type: String, amount: Double, category: String, note: String?) {
        val userId = currentUserId() ?: error("Not signed in")
        table.insert(NewTransaction(userId, type, amount, category, note))
    }

    suspend fun delete(id: String) {
        table.delete { filter { eq("id", id) } }
    }
}
