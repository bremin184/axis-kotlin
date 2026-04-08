package com.axis.app.domain.util

/**
 * Extracts a stable keyword from a transaction message for rule matching.
 */
fun extractKeyword(message: String): String {
    val knownSources = listOf(
        "SAFARICOM",
        "M-SHWARI",
        "KPLC",
        "NAIVAS",
        "QUICKMART",
        "UBER",
        "BOLT",
        "TILL",
        "PAYBILL"
    )

    val upper = message.uppercase()

    return knownSources.firstOrNull {
        upper.contains(it)
    } ?: upper.split(" ").first()
}
