package com.you.clipper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
data class Word(val start: Double, val end: Double, val text: String)
data class Seg(val start: Double, val end: Double, val text: String)
data class Transcript(val segments: List<Seg>, val words: List<Word>)
object Transcriber {
    private val client = OkHttpClient.Builder().connectTimeout(30, TimeUnit.SECONDS).readTimeout(180, TimeUnit.SECONDS).build()
    suspend fun transcribe(wav: File, key: String): Transcript {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "a.wav", wav.asRequestBody("audio/wav".toMediaType()))
            .addFormDataPart("model", "whisper-large-v3")
            .addFormDataPart("response_format", "verbose_json")
            .addFormDataPart("timestamp_granularities[]", "segment")
            .addFormDataPart("timestamp_granularities[]", "word")
            .build()
        val req = Request.Builder().url("https://api.groq.com/openai/v1/audio/transcriptions").header("Authorization", "Bearer $key").post(body).build()
        client.newCall(req).execute().use { resp ->
            val txt = resp.body!!.string()
            check(resp.isSuccessful) { "Groq error: $txt" }
            val root = JSONObject(txt)
            val segsArr = root.optJSONArray("segments") ?: org.json.JSONArray()
            val segs = (0 until segsArr.length()).map {
                val o = segsArr.getJSONObject(it)
                Seg(o.getDouble("start"), o.getDouble("end"), o.getString("text").trim())
            }
            val words = mutableListOf<Word>()
            root.optJSONArray("words")?.let { wa ->
                for (i in 0 until wa.length()) {
                    val o = wa.getJSONObject(i)
                    words += Word(o.getDouble("start"), o.getDouble("end"), o.getString("word").trim())
                }
            }
            if (words.isEmpty()) {
                for (i in 0 until segsArr.length()) {
                    val sw = segsArr.getJSONObject(i).optJSONArray("words") ?: continue
                    for (j in 0 until sw.length()) {
                        val o = sw.getJSONObject(j)
                        words += Word(o.getDouble("start"), o.getDouble("end"), o.getString("word").trim())
                    }
                }
            }
            return Transcript(segs, words)
        }
    }
}
