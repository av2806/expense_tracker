package com.example.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import com.example.expensetracker.database.AppDatabase
import com.example.expensetracker.database.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "android.provider.Telephony.SMS_RECEIVED") {
            val bundle = intent.extras ?: return
            val pdus = bundle.get("pdus") as? Array<*> ?: return

            for (pdu in pdus) {
                try {
                    val smsMessage = SmsMessage.createFromPdu(pdu as ByteArray)
                    val messageBody = smsMessage.messageBody
                    val senderNumber = smsMessage.originatingAddress ?: "Unknown"
                    Log.d("SmsReceiver", "SMS received: $messageBody from $senderNumber")

                    // Try to extract transaction data
                    val (title, amount, category) = parseSMS(messageBody) ?: continue

                    // Save to database
                    if (context != null) {
                        saveTransactionToDatabase(context, title, amount, category, senderNumber)
                    }
                } catch (e: Exception) {
                    Log.e("SmsReceiver", "Error processing SMS", e)
                }
            }
        }
    }

    private fun parseSMS(message: String): Triple<String, Int, String>? {
        try {
            val lowerMessage = message.lowercase()

            // Extract amount
            val amountRegex = """(?:₹|rs|rupees)\s*(\d+)""".toRegex(RegexOption.IGNORE_CASE)
            val amountMatch = amountRegex.find(lowerMessage)
            val amount = amountMatch?.groupValues?.get(1)?.toIntOrNull() ?: return null

            // Determine category
            val category = when {
                "swiggy" in lowerMessage || "zomato" in lowerMessage -> "Food"
                "uber" in lowerMessage || "metro" in lowerMessage -> "Transport"
                "amazon" in lowerMessage || "flipkart" in lowerMessage -> "Shopping"
                else -> "Other"
            }

            // Extract title
            val title = when {
                "swiggy" in lowerMessage -> "Swiggy"
                "zomato" in lowerMessage -> "Zomato"
                "uber" in lowerMessage -> "Uber"
                "amazon" in lowerMessage -> "Amazon"
                "flipkart" in lowerMessage -> "Flipkart"
                else -> "Transaction"
            }

            return Triple(title, amount, category)
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error parsing SMS", e)
            return null
        }
    }

    private fun saveTransactionToDatabase(
        context: Context,
        title: String,
        amount: Int,
        category: String,
        phoneNumber: String
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val transaction = Transaction(
                    title = title,
                    amount = amount,
                    category = category,
                    source = "sms",
                    phoneNumber = phoneNumber
                )
                db.transactionDao().insertTransaction(transaction)
                Log.d("SmsReceiver", "Transaction saved: $title - ₹$amount")
            } catch (e: Exception) {
                Log.e("SmsReceiver", "Error saving transaction", e)
            }
        }
    }
}