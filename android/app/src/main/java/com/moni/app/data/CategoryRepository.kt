package com.moni.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
private data class NewCategory(val name: String, val kind: String, val icon: String)

/** Categories of the caller's workspace (workspace_id defaults server side). */
class CategoryRepository {
    private val table get() = supabase.from("categories")

    /** Active and archived. */
    suspend fun listAll(): List<Category> =
        table.select { order("name", Order.ASCENDING) }.decodeList<Category>()

    suspend fun create(name: String, kind: String, icon: String) {
        table.insert(NewCategory(name, kind, icon))
    }

    suspend fun update(id: String, name: String, icon: String) {
        table.update({ set("name", name); set("icon", icon) }) { filter { eq("id", id) } }
    }

    suspend fun setArchived(id: String, archived: Boolean) {
        table.update({ set("archived", archived) }) { filter { eq("id", id) } }
    }
}
