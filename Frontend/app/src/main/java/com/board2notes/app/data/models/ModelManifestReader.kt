package com.board2notes.app.data.models

import android.content.Context
import org.json.JSONObject

data class ModelManifestEntry(
    val id: String,
    val name: String,
    val format: String,
    val runtimeSupported: Boolean,
    val path: String?,
    val inputName: String?,
    val outputNames: List<String>
)

class ModelManifestReader(private val context: Context) {
    fun read(): List<ModelManifestEntry> {
        val json = context.assets.open("models/model_manifest.json").bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        val models = root.getJSONArray("models")
        return List(models.length()) { index ->
            val obj = models.getJSONObject(index)
            val outputJson = obj.optJSONArray("output_names")
            ModelManifestEntry(
                id = obj.getString("id"),
                name = obj.getString("name"),
                format = obj.getString("format"),
                runtimeSupported = obj.optBoolean("runtime_supported", false),
                path = obj.optString("path").takeIf { it.isNotBlank() && it != "null" },
                inputName = obj.optString("input_name").takeIf { it.isNotBlank() },
                outputNames = if (outputJson == null) {
                    emptyList()
                } else {
                    List(outputJson.length()) { outputJson.getString(it) }
                }
            )
        }
    }
}
