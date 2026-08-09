package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object OpenAIService {
    private const val OPENAI_API_KEY = "sk-aW36gdTFRmQg6ESGDpMW8WFmpz9UdsvnZPdRst5ALEH0NZqR"
    private const val MODEL_NAME = "gpt-5.6-sol"
    private const val API_URL = "https://api.openai.com/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun scoreItem(context: android.content.Context, content: String, type: ItemType, dueAt: Long?): Int = withContext(Dispatchers.IO) {
        try {
            val dueText = if (type == ItemType.TODO && dueAt != null) {
                " The item is a To-Do with a due date timestamp of $dueAt."
            } else {
                ""
            }
            
            val systemPrompt = "You are an AI that scores the urgency and importance of a user's note, idea, or to-do item on a scale of 1 to 10. " +
                "10 is extremely urgent and time-sensitive (e.g. 'today', 'asap', explicit near deadlines). " +
                "Also factor in how actionable or consequential it sounds. " +
                "Return ONLY a single integer from 1 to 10. Do not return any other text."

            val userPrompt = "Score this item:\nContent: $content$dueText"

            val json = JSONObject().apply {
                put("model", MODEL_NAME)
                put("messages", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemPrompt)
                    })
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", userPrompt)
                    })
                })
                put("temperature", 0.3)
                put("max_tokens", 5)
            }

            val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(API_URL)
                .addHeader("Authorization", "Bearer $OPENAI_API_KEY")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                android.util.Log.d("ImportanceScoring", "Raw response: $responseBody")
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, "Raw response: $responseBody", android.widget.Toast.LENGTH_LONG).show()
                }
                
                if (response.isSuccessful && responseBody != null) {
                    val responseJson = JSONObject(responseBody)
                    val choices = responseJson.optJSONArray("choices")
                    if (choices != null && choices.length() > 0) {
                        val message = choices.getJSONObject(0).optJSONObject("message")
                        val textScore = message?.optString("content")?.trim()
                        if (textScore != null) {
                            // Extract integer
                            return@withContext textScore.toIntOrNull() ?: 5
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ImportanceScoring", "Exception in API call", e)
            withContext(Dispatchers.Main) {
                android.widget.Toast.makeText(context, "Exception: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            }
            e.printStackTrace()
        }
        return@withContext 5 // Default score on failure
    }
}
