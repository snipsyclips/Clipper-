package com.you.clipper
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
object Pipeline {
    suspend fun run(ctx: Context, srcUri: android.net.Uri, onStep: (String) -> Unit): List<File> = withContext(Dispatchers.IO) {
        val cfg = Settings(ctx)
        require(cfg.groqKey.isNotBlank() && cfg.geminiKey.isNotBlank()) { "Add API keys first" }
        val work = File(ctx.cacheDir, "work").apply { mkdirs() }
        val src = File(work, "input.mp4")
        ctx.contentResolver.openInputStream(srcUri)!!.use { i -> src.outputStream().use { o -> i.copyTo(o) } }
        onStep("Extracting audio…"); val wav = File(work, "audio.wav"); Ffmpeg.extractAudio(src, wav)
        onStep("Transcribing…"); val transcript = Transcriber.transcribe(wav, cfg.groqKey)
        if (transcript.segments.isEmpty()) error("No speech found")
        onStep("Finding viral moments…"); val moments = MomentFinder.find(transcript.segments, cfg.geminiKey).take(3)
        val outDir = File(ctx.getExternalFilesDir(null), "clips").apply { mkdirs() }
        val out = mutableListOf<File>()
        moments.forEachIndexed { i, m ->
            onStep("Rendering clip ${i + 1}/${moments.size}: ${m.title}")
            val wordsForClip = transcript.words.filter { it.end > m.start && it.start < m.end }
            val ass = if (wordsForClip.isEmpty()) null else File(work, "sub_$i.ass").apply { writeText(AssBuilder.build(wordsForClip, offset = m.start)) }
            val f = File(outDir, "clip_${i + 1}.mp4"); Ffmpeg.cutVertical(src, f, m.start, m.end, ass); out += f
        }
        onStep("Done"); out
    }
}
