package com.example.expensetracker

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository
    val allTransactions: Flow<List<Transaction>>
    val totalExpenses: Flow<Int?>
    val allCategories: Flow<List<Category>>

    private val prefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)
    
    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _geminiModel = MutableStateFlow(prefs.getString("gemini_model", "gemini-1.5-flash") ?: "gemini-1.5-flash")
    val geminiModel: StateFlow<String> = _geminiModel.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        val transactionDao = db.transactionDao()
        val categoryDao = db.categoryDao()
        val merchantMappingDao = db.merchantMappingDao()

        repository = TransactionRepository(transactionDao, categoryDao, merchantMappingDao)
        allTransactions = repository.allTransactions
        totalExpenses = repository.totalExpenses
        allCategories = repository.allCategories
    }

    fun addTransaction(title: String, amount: Int, category: String, paymentMethod: String = "Cash", bankName: String = "", bankLast4: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedCategory = category.trim()
            val finalCategory = if (trimmedCategory.isEmpty()) "Other" else {
                trimmedCategory.substring(0, 1).uppercase(java.util.Locale.ROOT) + trimmedCategory.substring(1)
            }
            val existing = repository.getCategoryByName(finalCategory)
            if (existing == null) {
                val newColor = Category.getBeautifulColorForCategory(finalCategory)
                repository.insertCategory(Category(name = finalCategory, color = newColor, isCustom = true))
                LogManager.log("INFO", "Dynamically created new category: $finalCategory with color $newColor")
            }

            val transaction = Transaction(
                title = title,
                amount = amount,
                category = finalCategory,
                paymentMethod = paymentMethod,
                bankName = bankName,
                bankLast4 = bankLast4
            )
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch(Dispatchers.IO) {
            val trimmedCategory = transaction.category.trim()
            val finalCategory = if (trimmedCategory.isEmpty()) "Other" else {
                trimmedCategory.substring(0, 1).uppercase(java.util.Locale.ROOT) + trimmedCategory.substring(1)
            }
            val existing = repository.getCategoryByName(finalCategory)
            if (existing == null) {
                val newColor = Category.getBeautifulColorForCategory(finalCategory)
                repository.insertCategory(Category(name = finalCategory, color = newColor, isCustom = true))
                LogManager.log("INFO", "Dynamically created new category from edit: $finalCategory with color $newColor")
            }
            repository.updateTransaction(transaction.copy(category = finalCategory))
        }
    }

    fun getTotalByPaymentMethod(method: String): Flow<Int?> {
        return repository.getTotalByPaymentMethod(method)
    }

    fun getTransactionsByCategory(category: String): Flow<List<Transaction>> {
        return repository.getTransactionsByCategory(category)
    }

    fun getCategoryIdByKeyword(keyword: String): Int? {
        return repository.getCategoryIdByKeyword(keyword)
    }

    fun getConfidenceByKeyword(keyword: String): Int? {
        return repository.getConfidenceByKeyword(keyword)
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setGeminiApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
        _geminiApiKey.value = key
    }

    fun setGeminiModel(model: String) {
        prefs.edit().putString("gemini_model", model).apply()
        _geminiModel.value = model
    }

    fun processNaturalLanguageCommand(
        commandText: String, 
        currentTransactions: List<Transaction>, 
        currentCategories: List<Category>,
        onResult: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val apiKey = _geminiApiKey.value
            val modelName = _geminiModel.value
            if (apiKey.isBlank()) {
                onResult("Error: Gemini API Key is not set. Please configure it in settings.")
                return@launch
            }
            
            val contextJson = JSONArray()
            currentTransactions.forEach { tx ->
                contextJson.put(JSONObject().apply {
                    put("id", tx.id)
                    put("title", tx.title)
                    put("amount", tx.amount)
                    put("category", tx.category)
                    put("paymentMethod", tx.paymentMethod)
                })
            }

            val categoriesList = currentCategories.map { it.name }.joinToString(", ")

            val systemInstruction = """
                You are an expense tracker AI assistant. You can add transactions, delete transactions, edit transactions, or reply to queries.
                Available categories: $categoriesList.
                Available payment methods: Cash, Card, UPI, Bank Transfer.
                Current transactions: ${contextJson.toString()}
                
                The user will give you a natural language instruction. Perform the action they requested by returning a JSON object matching this schema:
                {
                  "action": "add" | "delete" | "edit" | "reply",
                  "transactionToAdd": {
                     "title": "Merchant/Store/Description",
                     "amount": integer_amount,
                     "category": "strictly choose the best fitting category from the available categories list based on the transaction details (e.g. food apps -> Food, bus/taxi -> Transport, utilities/bills -> Bills). If none of the available categories fit the transaction context reasonably, you can define/create a new category name (use a short, single-word title-cased name like 'Medical', 'Travel', 'Education', 'Investment', etc.). Do NOT use 'Other' if a more specific category fits or can be created.",
                     "paymentMethod": "one of the available payment methods. Default to 'Cash' if not explicitly specified by user.",
                     "bankName": "optional name of bank if mentioned, e.g. HDFC, SBI",
                     "bankLast4": "optional last 4 digits of card/account if mentioned, e.g. 1234"
                  },
                  "transactionToDelete": {
                     "id": integer_id_of_transaction_to_delete
                  },
                  "transactionToEdit": {
                     "id": integer_id_of_transaction_to_edit,
                     "title": "New Merchant/Store/Description",
                     "amount": new_integer_amount,
                     "category": "new category matching available categories, or a new custom category if none fits",
                     "paymentMethod": "new payment method matching available payment methods",
                     "bankName": "new bank name",
                     "bankLast4": "new last 4 digits of card/account"
                  },
                  "replyMessage": "Confirmation message or answer to user query"
                }
                
                Guidelines:
                - If user wants to add, set action="add", fill transactionToAdd, and set replyMessage to a confirmation.
                - If they want to delete, locate the transaction in the current list, set action="delete", set transactionToDelete.id, and set replyMessage.
                - If they want to edit/modify/update/change a transaction, locate it, set action="edit", set transactionToEdit (only fill fields that are being modified, keeping other fields unchanged), and set replyMessage to a confirmation.
                - For calculations, balances, or queries, set action="reply" and answer in replyMessage.
                - Return ONLY the raw JSON object. No markdown backticks, no explanatory text.
            """.trimIndent()

            LogManager.log("API_REQ", "NL Command: \"$commandText\" using model $modelName")

            val responseJson = callGeminiAPIForNL(commandText, systemInstruction, modelName, apiKey)
            if (responseJson == null) {
                onResult("Error contacting Gemini API. Check your internet connection, model choice, or API key.")
                return@launch
            }

            try {
                val action = responseJson.optString("action", "reply")
                val replyMessage = responseJson.optString("replyMessage", "")

                LogManager.log("API_RES", "NL Result: Action=$action, Reply=\"$replyMessage\"")

                when (action) {
                    "add" -> {
                        val addObj = responseJson.optJSONObject("transactionToAdd")
                        if (addObj != null) {
                            val title = addObj.optString("title", "Expense")
                            val amount = addObj.optInt("amount", 0)
                            val category = addObj.optString("category", "Other")
                            val paymentMethod = addObj.optString("paymentMethod", "Cash")
                            val bankName = addObj.optString("bankName", "")
                            val bankLast4 = addObj.optString("bankLast4", "")
                            addTransaction(title, amount, category, paymentMethod, bankName, bankLast4)
                        }
                    }
                    "delete" -> {
                        val delObj = responseJson.optJSONObject("transactionToDelete")
                        if (delObj != null) {
                            val idToDelete = delObj.optInt("id", -1)
                            val matchingTx = currentTransactions.find { it.id == idToDelete }
                            if (matchingTx != null) {
                                deleteTransaction(matchingTx)
                            }
                        }
                    }
                    "edit" -> {
                        val editObj = responseJson.optJSONObject("transactionToEdit")
                        if (editObj != null) {
                            val idToEdit = editObj.optInt("id", -1)
                            val matchingTx = currentTransactions.find { it.id == idToEdit }
                            if (matchingTx != null) {
                                val title = editObj.optString("title", matchingTx.title)
                                val amount = editObj.optInt("amount", matchingTx.amount)
                                val category = editObj.optString("category", matchingTx.category)
                                val paymentMethod = editObj.optString("paymentMethod", matchingTx.paymentMethod)
                                val bankName = editObj.optString("bankName", matchingTx.bankName)
                                val bankLast4 = editObj.optString("bankLast4", matchingTx.bankLast4)
                                
                                val updatedTx = matchingTx.copy(
                                    title = title,
                                    amount = amount,
                                    category = category,
                                    paymentMethod = paymentMethod,
                                    bankName = bankName,
                                    bankLast4 = bankLast4
                                )
                                updateTransaction(updatedTx)
                            }
                        }
                    }
                }
                onResult(replyMessage)
            } catch (e: Exception) {
                LogManager.log("ERROR", "Failed to execute parsed command: ${e.message}")
                onResult("Failed to execute command: ${e.message}")
            }
        }
    }

    private fun callGeminiAPIForNL(text: String, systemInstruction: String, model: String, apiKey: String): JSONObject? {
        try {
            val urlString = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey"
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val jsonPayload = JSONObject().apply {
                put("contents", JSONArray().put(
                    JSONObject().apply {
                        put("parts", JSONArray().put(
                            JSONObject().apply {
                                put("text", "System Instruction: $systemInstruction\n\nUser Command: \"$text\"")
                            }
                        ))
                    }
                ))
            }

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
                        
                        return JSONObject(generatedText)
                    }
                }
            } else {
                val errorString = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                LogManager.log("ERROR", "Gemini API Error $responseCode: $errorString")
            }
        } catch (e: Exception) {
            LogManager.log("ERROR", "Connection failed: ${e.message}")
        }
        return null
    }

    fun fetchAvailableModels(apiKey: String, onResult: (List<String>) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            if (apiKey.isBlank()) {
                onResult(emptyList())
                return@launch
            }
            
            try {
                val urlString = "https://generativelanguage.googleapis.com/v1beta/models?key=$apiKey"
                val url = URL(urlString)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("Content-Type", "application/json")

                val responseCode = conn.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val responseString = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(responseString)
                    val modelsArray = json.optJSONArray("models")
                    val modelsList = mutableListOf<String>()
                    if (modelsArray != null) {
                        for (i in 0 until modelsArray.length()) {
                            val modelObj = modelsArray.getJSONObject(i)
                            val name = modelObj.optString("name", "")
                            val methods = modelObj.optJSONArray("supportedGenerationMethods")
                            var supportsGenerate = false
                            if (methods != null) {
                                for (j in 0 until methods.length()) {
                                    if (methods.getString(j) == "generateContent") {
                                        supportsGenerate = true
                                        break
                                    }
                                }
                            }
                            if (supportsGenerate && name.isNotBlank()) {
                                val cleanName = if (name.startsWith("models/")) name.substringAfter("models/") else name
                                modelsList.add(cleanName)
                            }
                        }
                    }
                    modelsList.sort()
                    onResult(modelsList)
                } else {
                    val errorString = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                    LogManager.log("ERROR", "Failed to fetch model list: $errorString")
                    onResult(emptyList())
                }
            } catch (e: Exception) {
                LogManager.log("ERROR", "Failed to retrieve available models: ${e.message}")
                onResult(emptyList())
            }
        }
    }
}