package com.example.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object BlockSerializer {

    fun toJson(blocks: List<ContentBlock>): String {
        return try {
            val jsonArray = JSONArray()
            blocks.forEach { block ->
                val jsonObj = JSONObject().apply {
                    put("id", block.id)
                    put("type", block.type)
                    put("text", block.text)
                }
                jsonArray.put(jsonObj)
            }
            jsonArray.toString()
        } catch (e: Exception) {
            "[]"
        }
    }

    fun toList(jsonStr: String?): List<ContentBlock> {
        if (jsonStr.isNullOrBlank()) {
            return listOf(ContentBlock(id = UUID.randomUUID().toString(), type = "PARAGRAPH", text = ""))
        }
        val list = mutableListOf<ContentBlock>()
        try {
            val jsonArray = JSONArray(jsonStr)
            for (i in 0 until jsonArray.length()) {
                val jsonObj = jsonArray.getJSONObject(i)
                list.add(
                    ContentBlock(
                        id = jsonObj.optString("id", UUID.randomUUID().toString()),
                        type = jsonObj.optString("type", "PARAGRAPH"),
                        text = jsonObj.optString("text", "")
                    )
                )
            }
        } catch (e: Exception) {
            // fallback
            list.add(ContentBlock(id = UUID.randomUUID().toString(), type = "PARAGRAPH", text = ""))
        }
        if (list.isEmpty()) {
            list.add(ContentBlock(id = UUID.randomUUID().toString(), type = "PARAGRAPH", text = ""))
        }
        return list
    }
}
