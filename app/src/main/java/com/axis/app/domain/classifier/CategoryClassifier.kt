package com.axis.app.domain.classifier

object CategoryClassifier {
    val CATEGORY_META: Map<String, CategoryMeta> = mapOf(
        "food_drink" to CategoryMeta("Food & Drink", "#4CAF50"),
        "transport" to CategoryMeta("Transport", "#2196F3"),
        "groceries" to CategoryMeta("Groceries", "#FF9800"),
        "utilities" to CategoryMeta("Utilities", "#9C27B0"),
        "shopping" to CategoryMeta("Shopping", "#E91E63"),
        "entertainment" to CategoryMeta("Entertainment", "#FFC107"),
        "airtime" to CategoryMeta("Airtime", "#03A9F4"),
        "personal" to CategoryMeta("Personal", "#607D8B"),
        "uncategorized" to CategoryMeta("Uncategorized", "#9E9E9E")
    )

    fun classify(type: String, recipient: String?, rawMessage: String, fundSubType: String): String {
        return "uncategorized"
    }
}

data class CategoryMeta(val label: String, val color: String)
