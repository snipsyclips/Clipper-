package com.you.clipper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
data class Moment(val start: Double, val end: Double, val title: String, val score: Int)
object MomentFinder {
    private val client = OkHttpClient.Builder().readTimeout(120, TimeUnit.SECONDS).build()
    suspend fun find(segments: List<Seg>, key: String): List<Moment> = withContext(Dispatchers.IO) {
        val transcript = segments.joinToString("\n") { "[${it.start.toInt()}-${it.end.toInt()}] ${it.text}" }
        val prompt = "You are a short-form video editor. From this timestamped transcript, pick the top 3 moments most likely to go viral as 15-60 second vertical clips. Prioritise: strong hooks, emotional spikes, self-contained ideas, punchy endings. Return ONLY JSON: [{\"start\":12,\"end\":45,\"title\":\"...\",\"score\":87}, ...]\nTranscript:\n$transcript"
        val payload = JSONObject().put("contents", org.json.JSONArray().put(JSONObject().put("parts", org.json.JSONArray().put(JSONObject().put("text", prompt))))).toString()
        val req = Request.Builder().url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=$key").post(payload.toRequestBody("application/json".toMediaType())).build()
        client.newCall(req).execute().use { resp ->
            val txt = resp.body!!.string()
            check(resp.isSuccessful) { "Gemini error: $txt" }
            val raw = JSONObject(txt).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").replace("```json", "").replace("```", "").trim()
            val arr = org.json.JSONArray(raw)
            return@withContext (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Moment(o.getDouble("start"), o.getDouble("end"), o.optString("title", "clip"), o.optInt("score", 0))
            }
        }
    }
}
