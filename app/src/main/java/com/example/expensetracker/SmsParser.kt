package com.example.expensetracker

data class ParsedSms(
    val amount: Int?,
    val merchant: String?,
    val fullText: String,
    val isValidTransaction: Boolean = true
)

object SmsParser {

    // Keywords that indicate SPENDING
    private val spendingKeywords = listOf(
        "spent", "deducted", "charged", "payment", "bill",
        "transaction", "purchase", "paid", "order", "booked",
        "transfer", "sent", "withdrawn", "debited"
    )

    // Keywords that indicate CREDITS (ignore these)
    // Keywords that indicate CREDITS or NOTIFICATIONS (ignore these)
    private val creditKeywords = listOf(
        "credited", "received", "refund", "balance", "available",
        "reward", "bonus", "cashback", "deposit", "remittance",
        "transferred to you", "has been generated", "is due", "bill for",
        "notification", "alert", "reminder", "statement"
    )

    fun parseSms(smsText: String): ParsedSms {
        val lowerText = smsText.lowercase()

        // Check if it's a credit/spam message (ignore it)
        val isCredit = creditKeywords.any { lowerText.contains(it) }
        if (isCredit) {
            return ParsedSms(
                amount = null,
                merchant = null,
                fullText = smsText,
                isValidTransaction = false
            )
        }

        // Check if it's a spending message
        val isSpending = spendingKeywords.any { lowerText.contains(it) }
        if (!isSpending) {
            // Not clearly a spending message, skip
            return ParsedSms(
                amount = null,
                merchant = null,
                fullText = smsText,
                isValidTransaction = false
            )
        }

        // Extract amount using regex (₹XXX or Rs XXX or INR XXX)
        val amountRegex = """(?:₹|Rs\.?|INR)\s*(\d+(?:,\d{3})*(?:\.\d{2})?)""".toRegex()
        val amountMatch = amountRegex.find(smsText)
        val amount = amountMatch?.groupValues?.get(1)?.replace(",", "")?.toIntOrNull()

        // Extract merchant name
        val merchantRegex = """(Swiggy|Amazon|Flipkart|Uber|Ola|Zomato|Zepto|Netflix|Spotify|ICICI|HDFC|Axis|SBI|Google Pay|PhonePe|Paytm|Wallet|Store|Shop|Instamart)""".toRegex(RegexOption.IGNORE_CASE)
        val merchantMatch = merchantRegex.find(smsText)
        val merchant = merchantMatch?.groupValues?.get(1)

        return ParsedSms(
            amount = amount,
            merchant = merchant,
            fullText = smsText,
            isValidTransaction = amount != null && amount > 0
        )
    }
}