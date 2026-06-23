package com.example.expensetracker

import java.util.regex.Pattern

object LocalRegexParser {

    data class ParsedTransaction(
        val title: String,
        val amount: Int,
        val category: String,
        val paymentMethod: String
    )

    
    private val amountPatterns = listOf(
        Pattern.compile("(?i)(?:rs\\.?|inr|inr\\.?)\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Pattern.compile("(?i)(?:debited|spent|paid|sent|withdrawn)\\s*(?:by|of)?\\s*(?:rs\\.?|inr)?\\s*([\\d,]+(?:\\.\\d{1,2})?)")
    )

    
    private val merchantPatterns = listOf(
        Pattern.compile("(?i)(?:at|to|in|on|with)\\s+([a-zA-Z0-9\\s]{3,25}?)(?:\\s+via|\\s+on|\\s+from|\\s+using|\\s+ending|\\s+account|\\s+a/c|\\.|$)")
    )

    
    private val categoryKeywords = mapOf(
        "Food" to listOf("starbucks", "swiggy", "zomato", "restaurant", "mcdonald", "kfc", "dominos", "pizza", "burger", "cafe", "coffee", "canteen", "grocery", "groceries", "supermarket"),
        "Transport" to listOf("uber", "ola", "rapido", "metro", "irctc", "railway", "train", "flight", "indigo", "fuel", "petrol", "diesel", "cng"),
        "Shopping" to listOf("amazon", "flipkart", "myntra", "meesho", "reliance", "zara", "h&m", "retail", "store", "mall", "decathlon", "shopping"),
        "Entertainment" to listOf("netflix", "spotify", "hotstar", "prime", "cinema", "movie", "theatre", "bookmyshow", "game", "steam", "playstation", "xbox"),
        "Bills" to listOf("electricity", "water", "bill", "recharge", "jio", "airtel", "vi", "broadband", "wifi", "rent", "insurance", "tata play")
    )

    fun parseNotification(text: String): ParsedTransaction? {
        val amount = extractAmount(text) ?: return null
        val merchant = extractMerchant(text) ?: "Auto Expense"
        val category = determineCategory(merchant, text)
        val paymentMethod = determinePaymentMethod(text)

        return ParsedTransaction(
            title = merchant,
            amount = amount,
            category = category,
            paymentMethod = paymentMethod
        )
    }

    private fun extractAmount(text: String): Int? {
        for (pattern in amountPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val rawAmount = matcher.group(1)?.replace(",", "")
                val parsedDouble = rawAmount?.toDoubleOrNull()
                if (parsedDouble != null && parsedDouble > 0) {
                    return parsedDouble.toInt()
                }
            }
        }
        return null
    }

    private fun extractMerchant(text: String): String? {
        for (pattern in merchantPatterns) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                val rawMerchant = matcher.group(1)?.trim()
                if (!rawMerchant.isNullOrBlank() && !isStopWord(rawMerchant)) {
                    return rawMerchant.split(" ").joinToString(" ") { 
                        it.lowercase().replaceFirstChar { char -> char.titlecase() } 
                    }
                }
            }
        }
        return null
    }

    private fun isStopWord(word: String): Boolean {
        val stopWords = setOf("my", "your", "the", "successful", "completed", "failed", "pending")
        return stopWords.contains(word.lowercase())
    }

    private fun determineCategory(merchant: String, text: String): String {
        val searchSource = "$merchant $text".lowercase()
        for ((category, keywords) in categoryKeywords) {
            for (keyword in keywords) {
                if (searchSource.contains(keyword)) {
                    return category
                }
            }
        }
        return "Other"
    }

    private fun determinePaymentMethod(text: String): String {
        val lowerText = text.lowercase()
        return when {
            lowerText.contains("upi") || lowerText.contains("gpay") || lowerText.contains("phonepe") || lowerText.contains("paytm") -> "UPI"
            lowerText.contains("card") || lowerText.contains("visa") || lowerText.contains("mastercard") || lowerText.contains("credit") || lowerText.contains("debit card") -> "Card"
            lowerText.contains("bank") || lowerText.contains("transfer") || lowerText.contains("neft") || lowerText.contains("rtgs") || lowerText.contains("imps") || lowerText.contains("a/c") -> "Bank Transfer"
            else -> "Cash"
        }
    }
}
