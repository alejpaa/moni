package com.moni.app.data

import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Profile(
    val id: String,
    val email: String? = null,
    @SerialName("full_name") val fullName: String? = null,
)

class ProfileRepository {
    private val columns = Columns.list("id", "email", "full_name")

    private fun userId(): String =
        supabase.auth.currentUserOrNull()?.id ?: error("Sesión no iniciada")

    suspend fun load(): Profile? =
        supabase.from("profiles").select(columns) { filter { eq("id", userId()) } }.decodeSingleOrNull<Profile>()

    suspend fun updateName(name: String) {
        supabase.from("profiles").update({ set("full_name", name) }) { filter { eq("id", userId()) } }
    }

    suspend fun updatePassword(password: String) {
        supabase.auth.updateUser { this.password = password }
    }
}
