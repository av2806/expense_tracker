package com.example.expensetracker.receiver

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.app.Notification
import android.content.Context
import android.util.Log
import com.example.expensetracker.AppDatabase
import com.example.expensetracker.Transaction
import com.example.expensetracker.Category
import com.example.expensetracker.LogManager
import com.example.expensetracker.LocalRegexParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import androidx.core.app.NotificationCompat

class NotificationListener : NotificationListenerService() {

    private val TAG = "NotificationListener"
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        LogManager.log("INFO", "Notification Listener Service CONNECTED and ACTIVE.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
        LogManager.log("INFO", "Notification Listener Service DISCONNECTED.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn.packageName ?: ""
        if (packageName == this.packageName) {
            return
        }
        
        if ((sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0) {
            return
        }
        
        val extras = sbn.notification.extras ?: android.os.Bundle()
        val title = extras.getString(Notification.EXTRA_TITLE)?.toString() ?: ""

        if (!isValidSender(packageName, title)) {
            return
        }

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString() ?: ""

        var bestText = text
        if (bigText.length > bestText.length) {
            bestText = bigText
        }

        
        val messages = try {
            extras.get("android.messages") as? Array<*>
        } catch (e: Exception) {
            null
        }
        if (messages != null && messages.isNotEmpty()) {
            val lastMsg = messages.last()
            if (lastMsg is android.os.Bundle) {
                val msgText = lastMsg.getCharSequence("text")?.toString() ?: ""
                if (msgText.isNotBlank()) {
                    bestText = msgText
                }
            }
        }

        val fullText = "$title $bestText $subText".lowercase()
        val keywords = listOf(
            "spent", "debited", "charged", "paid", "rs", "inr", 
            "transferred", "credited", "transaction", "txn", 
            "debit", "withdrawn", "purchased", "purchase"
        )
        val isTransactionMessage = keywords.any { fullText.contains(it) }

        if (isTransactionMessage && bestText.isNotBlank()) {
            val uniqueKey = bestText
            if (isNotificationDuplicate(uniqueKey)) {
                Log.d(TAG, "Notification is a duplicate update. Skipping processing.")
                return
            }

            LogManager.log("INFO", "Notification intercepted from $packageName. Title: \"$title\", Text: \"$bestText\"")

            val prefs = getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)
            val apiKey = prefs.getString("gemini_api_key", "") ?: ""
            serviceScope.launch {
                processNotificationWithAI(applicationContext, bestText, apiKey)
            }
        } else if (bestText.isNotBlank()) {
            val uniqueKey = bestText
            if (!isNotificationDuplicate(uniqueKey)) {
                LogManager.log("INFO", "Non-transaction SMS intercepted from $packageName. Title: \"$title\", Text: \"$bestText\"")
            }
        }
    }

    companion object {
        private const val TAG = "NotificationListener"
        var isServiceConnected = false

        fun isValidSender(packageName: String, title: String): Boolean {
            val isSmsApp = packageName.contains("messaging", ignoreCase = true) || 
                           packageName.contains("mms", ignoreCase = true) || 
                           packageName.contains("sms", ignoreCase = true)
            if (!isSmsApp) return false

            val senderId = title.trim()
            return senderId.matches(Regex("^[a-zA-Z0-9\\-.]+$")) && 
                   senderId.any { it.isLetter() } && 
                   senderId.length >= 3
        }

        private val processedMessageTimes = java.util.Collections.synchronizedMap(HashMap<String, Long>())
        private val processedKeysQueue = java.util.concurrent.ConcurrentLinkedQueue<String>()

        private fun isNotificationDuplicate(messageText: String): Boolean {
            val normalized = messageText.replace(Regex("[^a-zA-Z0-9]"), "").lowercase()
            val now = System.currentTimeMillis()
            val lastProcessedTime = processedMessageTimes[normalized]
            if (lastProcessedTime != null && (now - lastProcessedTime < 15000L)) {
                return true
            }
            processedMessageTimes[normalized] = now
            processedKeysQueue.add(normalized)
            if (processedKeysQueue.size > 100) {
                val oldest = processedKeysQueue.poll()
                if (oldest != null) {
                    processedMessageTimes.remove(oldest)
                }
            }
            return false
        }

        suspend fun processNotificationWithAI(context: Context, messageText: String, apiKey: String) = withContext(Dispatchers.IO) {
            try {
                LogManager.log("INFO", "SMS Transaction Notification intercepted: \"$messageText\"")

                if (apiKey.isBlank()) {
                    LogManager.log("ERROR", "Gemini API Key is missing. Cannot parse SMS via AI. Skipping.")
                    return@withContext
                }

                LogManager.log("INFO", "Sending SMS notification to Gemini AI (API Key configured) for auto-categorization.")

                val prefs = context.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)
                val modelName = prefs.getString("gemini_model", "gemini-1.5-flash") ?: "gemini-1.5-flash"

                val db = AppDatabase.getDatabase(context)
                val categories = db.categoryDao().getAllCategoriesSync()
                val categoriesList = categories.joinToString(", ") { it.name }

                val parsedJson = callGeminiAPI(messageText, categoriesList, modelName, apiKey)
                if (parsedJson == null) {
                    LogManager.log("ERROR", "AI parser returned null. Transaction not logged.")
                    return@withContext
                }
                
                val title = parsedJson.optString("title", "Auto Expense")
                val amount = parsedJson.optInt("amount", 0)
                val category = parsedJson.optString("category", "Other")
                val paymentMethod = parsedJson.optString("paymentMethod", "Cash")
                val bankName = parsedJson.optString("bankName", "")
                val bankLast4 = parsedJson.optString("bankLast4", "")

                if (amount > 0) {
                    val duplicateLimit = System.currentTimeMillis() - 15000L
                    val recentTransactions = db.transactionDao().getTransactionsSince(duplicateLimit)
                    val isDuplicate = recentTransactions.any { tx ->
                        tx.amount == amount && (
                            tx.title.contains(title, ignoreCase = true) || 
                            title.contains(tx.title, ignoreCase = true)
                        )
                    }
                    if (isDuplicate) {
                        LogManager.log("INFO", "Duplicate transaction detected in DB (already logged within 15s): $title, ₹$amount. Skipping.")
                        return@withContext
                    }

                    val trimmedCategory = category.trim()
                    val finalCategory = if (trimmedCategory.isEmpty()) "Other" else {
                        trimmedCategory.substring(0, 1).uppercase(java.util.Locale.ROOT) + trimmedCategory.substring(1)
                    }
                    val existing = db.categoryDao().getCategoryByName(finalCategory)
                    if (existing == null) {
                        val newColor = Category.getBeautifulColorForCategory(finalCategory)
                        db.categoryDao().insertCategory(Category(name = finalCategory, color = newColor, isCustom = true))
                        LogManager.log("INFO", "SMS auto-logged transaction created new category: $finalCategory with color $newColor")
                    }

                    val transaction = Transaction(
                        title = title,
                        amount = amount,
                        category = finalCategory,
                        paymentMethod = paymentMethod,
                        source = "notification",
                        bankName = bankName,
                        bankLast4 = bankLast4
                    )
                    db.transactionDao().insertTransaction(transaction)

                    LogManager.log("INFO", "Auto-logged transaction via AI: $title, ₹$amount ($finalCategory, $paymentMethod, Bank: $bankName, Last4: $bankLast4)")
                } else {
                    LogManager.log("INFO", "AI parsed notification, but amount was 0 or not an expense. Ignored.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification with AI", e)
                LogManager.log("ERROR", "Failed to process notification: ${e.message}")
            }
        }

        private fun callGeminiAPI(text: String, categoriesList: String, model: String, apiKey: String): JSONObject? {
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val systemInstruction = "You are a financial transaction assistant. Analyze this notification/message text and extract the expense transaction details. Return ONLY a single raw JSON object (no markdown, no backticks, no explanatory text) with these fields: 'title' (merchant/store/person paid, e.g. Amazon, Starbucks, etc.), 'amount' (integer amount paid), 'category' (strictly choose the best fitting category from the available categories: $categoriesList. If none of the available categories fit the transaction context reasonably, you can define/create a new custom category name. Use a short, single-word title-cased name like 'Medical', 'Travel', 'Education', 'Investment', etc. Do NOT use 'Other' if a more specific category fits or can be created), 'paymentMethod' (strictly one of: Cash, Card, UPI, Bank Transfer. Default to 'Cash' if not specified or unclear), 'bankName' (the name of the bank/payment app if mentioned, e.g. HDFC, SBI, Paytm, etc., or empty string if not found), 'bankLast4' (last 4 digits of the account/card if mentioned, e.g. 1234, or empty string if not found). If the message is not a debit/expense transaction, return an empty JSON object {}."

                val jsonPayload = JSONObject().apply {
                    put("contents", org.json.JSONArray().put(
                        JSONObject().apply {
                            put("parts", org.json.JSONArray().put(
                                JSONObject().apply {
                                    put("text", "System Instruction: $systemInstruction\n\nNotification Text: \"$text\"")
                                }
                            ))
                        }
                    ))
                }

                LogManager.log("API_REQ", "Sent parse request using model $model")

                val writer = OutputStreamWriter(conn.outputStream)
                writer.write(jsonPayload.toString())
                writer.flush()
                writer.close()

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseString)
                    
                    val candidates = responseJson.optJSONArray("candidates")
                    if (candidates != null && candidates.length() > 0) {
                        val content = candidates.getJSONObject(0).optJSONObject("content")
                        val parts = content?.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            var generatedText = parts.getJSONObject(0).optString("text").trim()
                            
                            if (generatedText.startsWith("```json")) {
                                generatedText = generatedText.substringAfter("```json").substringBeforeLast("```").trim()
                            } else if (generatedText.startsWith("```")) {
                                generatedText = generatedText.substringAfter("```").substringBeforeLast("```").trim()
                            }
                            
                            LogManager.log("API_RES", "Response JSON: $generatedText")
                            return JSONObject(generatedText)
                        }
                    }
                } else {
                    val errorString = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    Log.e(TAG, "Gemini API Error: $responseCode - $errorString")
                    LogManager.log("ERROR", "Gemini API Error $responseCode: $errorString")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in callGeminiAPI", e)
                LogManager.log("ERROR", "API connection failed: ${e.message}")
            }
            return null
        }
    }
}
