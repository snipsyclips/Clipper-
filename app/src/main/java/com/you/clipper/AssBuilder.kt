package com.you.clipper
object AssBuilder {
    private const val ACCENT = "&H0000FFFF&"; private const val WHITE = "&H00FFFFFF&"; private const val MAX_WORDS = 3; private const val GAP_BREAK = 0.55
    private val HEADER = """
        [Script Info]
        ScriptType: v4.00+
        PlayResX: 1080
        PlayResY: 1920
        WrapStyle: 0
        ScaledBorderAndShadow: yes
        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,sans-serif,100,&H00FFFFFF,&H000000FF,&H00000000,&H00000000,-1,0,0,0,100,100,0,0,1,7,4,2,50,50,260,1
        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
    """.trimIndent()
    fun build(words: List<Word>, offset: Double): String {
        if (words.isEmpty()) return ""
        val sb = StringBuilder(HEADER)
        for (chunk in chunk(words)) {
            for (active in chunk.indices) {
                val w = chunk[active]; val start = w.start - offset; val end = w.end - offset
                if (end <= 0 || w.text.isBlank()) continue
                val line = chunk.mapIndexed { j, cw ->
                    val tag = if (j == active) "{\\c$ACCENT\\fscx112\\fscy112}" else "{\\c$WHITE\\fscx100\\fscy100}"
                    tag + esc(cw.text) + "{\\c$WHITE\\fscx100\\fscy100}"
                }.joinToString(" ")
                sb.append("Dialogue: 0,${ts(start)},${ts(end)},Default,,0,0,0,,{\\an2\\pos(540,1580)}$line\n")
            }
        }
        return sb.toString()
    }
    private fun chunk(words: List<Word>): List<List<Word>> {
        val out = mutableListOf<List<Word>>(); var cur = mutableListOf<Word>()
        for (w in words) {
            if (cur.isNotEmpty()) {
                val gap = w.start - cur.last().end
                val sentenceEnd = cur.last().text.lastOrNull()?.let { it in ".!?" } == true
                if (cur.size >= MAX_WORDS || gap > GAP_BREAK || sentenceEnd) { out += cur; cur = mutableListOf() }
            }
            cur += w
        }
        if (cur.isNotEmpty()) out += cur
        return out
    }
    private fun esc(s: String) = s.replace("\\", "").replace("{", "(").replace("}", ")").replace("\n", " ").trim()
    private fun ts(sec: Double): String { val s = sec.coerceAtLeast(0.0); return "%d:%02d:%05.2f".format((s / 3600).toInt(), ((s % 3600) / 60).toInt(), s % 60) }
}
