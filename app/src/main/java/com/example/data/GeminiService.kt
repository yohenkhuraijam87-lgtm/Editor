package com.example.data

import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun improveText(text: String, command: String): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "API Key is missing or default. Add your key into secrets.")
            return@withContext "Error: API Key is not set up in physical secrets. Please store your Gemini API Key in the AI Studio Secrets panel."
        }

        // Direct request mapping according to selected option (proofread, formal, simplify, improve)
        val systemPrompt = when (command.uppercase()) {
            "PROOFREAD" -> "You are a professional grammar and spelling proofreader. Analyze the provided text. Correct errors in spelling, punctuation, capitalization, and grammar. Return the corrected and improved text ONLY. Do NOT wrap under markdown block or add warnings or explanations unless critical. Keep formatting identical."
            "FORMAL" -> "You are an executive editor. Rewrite the provided text with a highly professional, polite, and persuasive academic/formal tone. Return the rewritten text ONLY."
            "SIMPLIFY" -> "You are a writing editor. Simplify the provided text to make it extremely clear, active, concise, and easy to read. Eliminate passive voice and corporate clutter. Return the simplified text ONLY."
            "IMPROVE" -> "You are an experienced document designer. Enhance the style, rhythm, vocabulary, and flow of the text while retaining its core narrative. Return the polished text ONLY."
            else -> "Correct spelling, grammar, and typos, polished for an A4 Document. Return the cleaned text ONLY."
        }

        val requestUrl = "$BASE_URL/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"

        val rootObject = JSONObject().apply {
            val contentArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", "Text to analyze: $text")
                        })
                    })
                })
            }
            put("contents", contentArray)

            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply {
                        put("text", systemPrompt)
                    })
                })
            })

            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3) // lower temperature for editorial/spelling precision
            })
        }

        val requestBody = rootObject.toString().toRequestBody(jsonMediaType)
        val request = Request.Builder()
            .url(requestUrl)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val code = response.code
                    val errorMsg = response.body?.string() ?: ""
                    Log.e(TAG, "Unsuccessful response from Gemini: $code - $errorMsg")
                    return@withContext "Error: $code - $errorMsg"
                }

                val bodyStr = response.body?.string() ?: return@withContext "Error: Private response body"
                try {
                    val rootJson = JSONObject(bodyStr)
                    val candidates = rootJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val contentObj = candidates.getJSONObject(0).optJSONObject("content")
                        if (contentObj != null) {
                            val parts = contentObj.optJSONArray("parts")
                            if (parts != null && parts.length() > 0) {
                                return@withContext parts.getJSONObject(0).optString("text", "No text candidate").trim()
                            }
                        }
                    }
                    return@withContext "No response formatting from AI models."
                } catch (e: Exception) {
                    Log.e(TAG, "Failed parsing JSON response: ${e.message}", e)
                    return@withContext "Error parsing response: ${e.localizedMessage}"
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network timeout or error calling Gemini API: ${e.message}", e)
            return@withContext "Network Connection Failed. Check internet or API credentials. details: ${e.localizedMessage}"
        }
    }
}
