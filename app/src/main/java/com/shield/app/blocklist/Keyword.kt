package com.shield.app.blocklist

/**
 * Wraps a raw pattern string with a compiled, case-insensitive Kotlin
 * [Regex]. Invalid patterns are treated as literal text so a malformed
 * user-entered pattern can never crash matching.
 */
class Keyword(val rawPattern: String) {

    val regex: Regex = try {
        Regex(rawPattern, RegexOption.IGNORE_CASE)
    } catch (e: Exception) {
        Regex(Regex.escape(rawPattern), RegexOption.IGNORE_CASE)
    }

    fun matches(text: String): Boolean = regex.containsMatchIn(text)

    override fun equals(other: Any?): Boolean =
        other is Keyword && other.rawPattern == rawPattern

    override fun hashCode(): Int = rawPattern.hashCode()
}
