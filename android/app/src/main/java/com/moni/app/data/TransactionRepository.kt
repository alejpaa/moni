package com.moni.app.data

import io.github.jan.supabase.auth.SignOutScope
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class TransactionRepository {
    private val table get() = supabase.from("transactions")

    private val txColumns = Columns.list(
        "id", "workspace_id", "user_id", "type", "amount", "category_id",
        "account_id", "merchant", "source", "status", "occurred_at", "note", "reminder_id",
    )

    fun currentUserId(): String? = supabase.auth.currentUserOrNull()?.id
    fun currentEmail(): String? = supabase.auth.currentUserOrNull()?.email

    suspend fun signIn(email: String, password: String) =
        supabase.auth.signInWith(io.github.jan.supabase.auth.providers.builtin.Email) {
            this.email = email
            this.password = password
        }

    suspend fun signUp(email: String, password: String) {
        supabase.auth.signUpWith(io.github.jan.supabase.auth.providers.builtin.Email) {
            this.email = email
            this.password = password
        }
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            supabase.auth.signOut(SignOutScope.LOCAL)
        }
    }

    suspend fun listPage(workspaceId: String, offset: Long = 0L, pageSize: Long = PAGE_SIZE): List<Transaction> =
        table.select(txColumns) {
            filter {
                eq("workspace_id", workspaceId)
                eq("status", "confirmed")
            }
            order("occurred_at", Order.DESCENDING)
            order("id", Order.DESCENDING)
            range(offset, offset + pageSize - 1)
        }.decodeList()

    suspend fun listPending(workspaceId: String): List<Transaction> =
        table.select(txColumns) {
            filter {
                eq("workspace_id", workspaceId)
                eq("status", "pending")
            }
            order("occurred_at", Order.DESCENDING)
            order("id", Order.DESCENDING)
            limit(PAGE_SIZE)
        }.decodeList()

    suspend fun add(
        workspaceId: String,
        type: String,
        amount: Double,
        categoryId: String,
        accountId: String?,
        note: String?,
        reminderId: String? = null,
    ): Transaction =
        table.insert(
            NewTransaction(
                workspaceId = workspaceId,
                type = type,
                amount = round2(amount),
                categoryId = categoryId,
                accountId = accountId,
                note = note,
                reminderId = reminderId,
            ),
        ) {
            select(txColumns)
        }.decodeSingle()

    /** Confirms or ignores a pending/installment row, optionally reassigning category/account. */
    suspend fun review(
        workspaceId: String,
        id: String,
        status: String,
        categoryId: String? = null,
        accountId: String? = null,
    ): Transaction {
        // Built by hand so unchanged fields are never sent (a null would clear the column).
        val patch = buildJsonObject {
            put("status", status)
            categoryId?.let { put("category_id", it) }
            accountId?.let { put("account_id", it) }
        }
        return table.update(patch) {
            filter {
                eq("id", id)
                eq("workspace_id", workspaceId)
            }
            select(txColumns)
        }.decodeSingle()
    }

    suspend fun delete(workspaceId: String, id: String) {
        table.delete {
            filter {
                eq("id", id)
                eq("workspace_id", workspaceId)
            }
        }
    }

    companion object {
        const val PAGE_SIZE: Long = 50L
    }
}
