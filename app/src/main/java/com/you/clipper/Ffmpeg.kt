package com.you.clipper
import com.arthenica.ffmpegkit.FFmpegKit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
object Ffmpeg {
    suspend fun extractAudio(input: File, out: File) = withContext(Dispatchers.IO) {
        val cmd = "-y -i \"${input.absolutePath}\" -vn -ac 1 -ar 16000 \"${out.absolutePath}\""
        val s = FFmpegKit.execute(cmd)
        check(s.returnCode.isValueSuccess) { "audio extract failed: ${s.failStackTrace}" }
    }
    suspend fun cutVertical(input: File, out: File, start: Double, end: Double, assFile: File?) =
        withContext(Dispatchers.IO) {
            val dur = end - start
            val vf = buildString {
                append("crop=ih*9/16:ih,scale=1080:1920")
                if (assFile != null) {
                    val p = assFile.absolutePath.replace("\\", "/").replace("'", "\\'")
                    append(",ass='$p'")
                }
            }
            val cmd = "-y -ss $start -t $dur -i \"${input.absolutePath}\" " +
                      "-vf \"$vf\" -c:v libx264 -preset ultrafast -crf 28 " +
                      "-c:a aac -b:a 96k -movflags +faststart \"${out.absolutePath}\""
            val s = FFmpegKit.execute(cmd)
            check(s.returnCode.isValueSuccess) { "clip failed: ${s.failStackTrace}" }
        }
}
