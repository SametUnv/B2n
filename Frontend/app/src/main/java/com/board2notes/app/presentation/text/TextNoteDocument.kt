package com.board2notes.app.presentation.text

import org.json.JSONArray
import org.json.JSONObject

sealed class TextNoteBlock {
    data class Text(val text: String) : TextNoteBlock()
    data class Image(val asset: String) : TextNoteBlock()
}

object TextNoteCodec {
    const val MARKER = "[[B2N_TEXT_V1]]"
    private const val FULL_MARKER = "\n\n$MARKER\n"

    fun serialize(blocks: List<TextNoteBlock>): String {
        val plain = buildString {
            blocks.forEachIndexed { i, b ->
                if (b is TextNoteBlock.Text) {
                    if (i > 0 && b.text.isNotEmpty()) append("\n")
                    append(b.text)
                }
            }
        }
        val hasImage = blocks.any { it is TextNoteBlock.Image }
        if (!hasImage) return plain
        val arr = JSONArray()
        blocks.forEach { b ->
            val obj = JSONObject()
            when (b) {
                is TextNoteBlock.Text -> {
                    obj.put("type", "text")
                    obj.put("text", b.text)
                }
                is TextNoteBlock.Image -> {
                    obj.put("type", "image")
                    obj.put("asset", b.asset)
                }
            }
            arr.put(obj)
        }
        return plain + FULL_MARKER + arr.toString()
    }

    fun parse(body: String): List<TextNoteBlock> {
        val idx = body.indexOf(MARKER)
        if (idx < 0) return listOf(TextNoteBlock.Text(body))
        val jsonStart = idx + MARKER.length
        val json = body.substring(jsonStart).trimStart('\n', '\r', ' ')
        return runCatching {
            val arr = JSONArray(json)
            val out = mutableListOf<TextNoteBlock>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                when (obj.optString("type")) {
                    "text" -> out += TextNoteBlock.Text(obj.optString("text", ""))
                    "image" -> {
                        val asset = obj.optString("asset", "")
                        if (asset.isNotEmpty()) out += TextNoteBlock.Image(asset)
                    }
                }
            }
            if (out.isEmpty()) listOf(TextNoteBlock.Text(stripMarker(body))) else out
        }.getOrElse { listOf(TextNoteBlock.Text(stripMarker(body))) }
    }

    fun stripMarker(body: String): String {
        val idx = body.indexOf(MARKER)
        if (idx < 0) return body
        return body.substring(0, idx).trimEnd('\n', '\r', ' ')
    }
}
