package com.moni.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** Editable fields of public.accounts. Card fields must be null unless [type] is credit_card. */
data class AccountInput(
    val name: String,
    val type: String,
    val openingBalance: Double,
    val institution: String?,
    val last4: String?,
    val creditLimit: Double?,
    val closingDay: Int?,
    val dueDay: Int?,
)

@Serializable
data class AccountBalance(
    @SerialName("account_id") val accountId: String,
    val type: String,
    val balance: Double = 0.0,
    @SerialName("card_debt") val cardDebt: Double? = null,
    @SerialName("pending_installments") val pendingInstallments: Double = 0.0,
    @SerialName("available_credit") val availableCredit: Double? = null,
) {
    /** For credit cards this is the debt owed (positive). */
    val effectiveBalance: Double get() = if (type == "credit_card") cardDebt ?: 0.0 else balance
}

private val excludedFromDisponible = setOf("savings", "investment", "credit_card")

data class BalanceTotals(
    val disponible: Double,
    val cardDebt: Double,
    val hasCards: Boolean,
)

/**
 * Computes Disponible and total credit-card debt from active (non-archived) accounts:
 * Disponible = sum(balances of non-savings, non-investment, non-credit_card) - sum(credit_card debt).
 */
fun computeBalanceTotals(accounts: List<Account>, balances: Map<String, AccountBalance>): BalanceTotals {
    val active = accounts.filter { !it.archived }
    val liquid = active
        .filter { it.type !in excludedFromDisponible }
        .sumOf { balances[it.id]?.effectiveBalance ?: it.openingBalance }
    val cards = active.filter { it.type == "credit_card" }
    val cardDebt = cards.sumOf { balances[it.id]?.effectiveBalance ?: it.openingBalance }
    return BalanceTotals(
        disponible = round2(liquid - cardDebt),
        cardDebt = round2(cardDebt),
        hasCards = cards.isNotEmpty(),
    )
}

class AccountRepository {
    private val table get() = supabase.from("accounts")

    suspend fun list(workspaceId: String, includeArchived: Boolean = true): List<Account> =
        table.select(
            Columns.list(
                "id", "name", "type", "opening_balance", "institution", "last4", "color",
                "credit_limit", "closing_day", "due_day", "archived",
            ),
        ) {
            filter {
                eq("workspace_id", workspaceId)
                if (!includeArchived) eq("archived", false)
            }
            order("created_at", Order.ASCENDING)
        }.decodeList()

    suspend fun create(workspaceId: String, input: AccountInput) {
        table.insert(body(input, workspaceId))
    }

    suspend fun update(workspaceId: String, id: String, input: AccountInput) {
        table.update(body(input, workspaceId = null)) {
            filter {
                eq("id", id)
                eq("workspace_id", workspaceId)
            }
        }
    }

    suspend fun setArchived(workspaceId: String, id: String, archived: Boolean) {
        table.update(buildJsonObject { put("archived", archived) }) {
            filter {
                eq("id", id)
                eq("workspace_id", workspaceId)
            }
        }
    }

    /**
     * Balance and credit card usage per account id from the `account_balances` view.
     */
    suspend fun balances(workspaceId: String): Map<String, AccountBalance> =
        supabase.from("account_balances")
            .select(Columns.list("account_id", "type", "balance", "card_debt", "pending_installments", "available_credit")) {
                filter { eq("workspace_id", workspaceId) }
            }
            .decodeList<AccountBalance>()
            .associateBy { it.accountId }

    /** Explicit nulls: switching a card to another type must clear the card columns. */
    private fun body(i: AccountInput, workspaceId: String?): JsonObject = buildJsonObject {
        workspaceId?.let { put("workspace_id", it) }
        put("name", i.name)
        put("type", i.type)
        put("opening_balance", round2(i.openingBalance))
        put("institution", i.institution?.let { JsonPrimitive(it) } ?: JsonNull)
        val card = i.type == "credit_card"
        put("last4", if (card) i.last4?.let { JsonPrimitive(it) } ?: JsonNull else JsonNull)
        put("credit_limit", if (card && i.creditLimit != null) JsonPrimitive(round2(i.creditLimit)) else JsonNull)
        put("closing_day", if (card && i.closingDay != null) JsonPrimitive(i.closingDay) else JsonNull)
        put("due_day", if (card && i.dueDay != null) JsonPrimitive(i.dueDay) else JsonNull)
    }
}
