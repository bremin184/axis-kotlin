package com.axis.app.domain.budget

enum class SuggestionType {
    REALLOCATION,
    WARNING,
    SAVINGS_RATE
}

data class Suggestion(
    val type: SuggestionType,
    val message: String
)
