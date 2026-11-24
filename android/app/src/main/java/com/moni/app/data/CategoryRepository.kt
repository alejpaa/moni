package com.moni.app.data

import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
private data class NewCategory(
    @SerialName("workspace_id") val workspaceId: String,
    val name: String,
    val kind: String,
    val icon: String,
)

class CategoryRepository {
    private val table get() = supabase.from("categories")
    private val columns = Columns.list("id", "name", "kind", "icon", "archived")

    /** Active and archived. */
    suspend fun listAll(workspaceId: String): List<Category> =
        table.select(columns) {
            filter { eq("workspace_id", workspaceId) }
            order("name", Order.ASCENDING)
        }.decodeList()

    suspend fun create(workspaceId: String, name: String, kind: String, icon: String) {
        table.insert(NewCategory(workspaceId, name, kind, icon))
    }

    suspend fun update(workspaceId: String, id: String, name: String, icon: String) {
        table.update({ set("name", name); set("icon", icon) }) {
            filter {
                eq("id", id)
                eq("workspace_id", workspaceId)
            }
        }
    }

    suspend fun setArchived(workspaceId: String, id: String, archived: Boolean) {
        table.update({ set("archived", archived) }) {
            filter {
                eq("id", id)
                eq("workspace_id", workspaceId)
            }
        }
    }
}
