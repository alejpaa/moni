package com.moni.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneId

val LIMA_ZONE: ZoneId = ZoneId.of("America/Lima")

/** Money is numeric(12,2): keep two decimals via BigDecimal HALF_UP and drop floating-point noise. */
fun round2(v: Double): Double =
    if (!v.isFinite()) 0.0
    else BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).toDouble()

fun parseInstantOrNull(iso: String): Instant? =
    try {
        OffsetDateTime.parse(iso).toInstant()
    } catch (_: Exception) {
        try {
            Instant.parse(iso)
        } catch (_: Exception) {
            null
        }
    }

fun isInCurrentLimaMonth(iso: String): Boolean {
    val instant = parseInstantOrNull(iso) ?: return false
    val txMonth = YearMonth.from(instant.atZone(LIMA_ZONE))
    return txMonth == YearMonth.now(LIMA_ZONE)
}

@Serializable
data class WorkspaceMembership(
    @SerialName("workspace_id") val workspaceId: String,
    val role: String,
)

class WorkspaceRepository {
    /**
     * Resolves the caller's active workspace matching `private.current_workspace_id()`
     * (the user's oldest membership in `workspace_members`).
     */
    suspend fun resolveActiveWorkspace(): WorkspaceMembership {
        val userId = supabase.auth.currentUserOrNull()?.id ?: error("Sesión no iniciada")
        queryOldestMembership(userId)?.let { return it }
        supabase.postgrest.rpc("ensure_personal_workspace")
        return queryOldestMembership(userId) ?: error("No se encontró un espacio de trabajo activo")
    }

    private suspend fun queryOldestMembership(userId: String): WorkspaceMembership? =
        supabase.from("workspace_members")
            .select(Columns.list("workspace_id", "role")) {
                filter { eq("user_id", userId) }
                order("created_at", Order.ASCENDING)
                order("workspace_id", Order.ASCENDING)
                limit(1)
            }
            .decodeSingleOrNull()
}
