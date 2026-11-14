package com.you.clipper
import android.content.Context
class Settings(ctx: Context) {
    private val p = ctx.getSharedPreferences("cfg", Context.MODE_PRIVATE)
    var groqKey: String
        get() = p.getString("groq", "") ?: ""
        set(v) = p.edit().putString("groq", v).apply()
    var geminiKey: String
        get() = p.getString("gemini", "") ?: ""
        set(v) = p.edit().putString("gemini", v).apply()
}
