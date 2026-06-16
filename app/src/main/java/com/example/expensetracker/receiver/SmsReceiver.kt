package com.example.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expensetracker.AppDatabase
import com.example.expensetracker.NotificationHelper
import com.example.expensetracker.ParsedSms
import com.example.expensetracker.SmsParser
import com.example.expensetracker.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val smsMessages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            for (smsMessage in smsMessages) {
                val smsText = smsMessage.displayMessageBody
                val smsTimestamp = smsMessage.timestampMillis  // ← Get SMS timestamp
                val parsedSms = SmsParser.parseSms(smsText)

                if (parsedSms.isValidTransaction && parsedSms.amount != null && parsedSms.amount > 0) {
                    processParsedSms(context, parsedSms, smsTimestamp)  // ← Pass smsTimestamp!
                }
            }
        }
    }

    private fun processParsedSms(context: Context, parsedSms: ParsedSms, smsTimestamp: Long) {
        GlobalScope.launch(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(context)
            val merchantDao = db.merchantMappingDao()

            val merchant = parsedSms.merchant ?: "Other"
            val categoryId = merchantDao.getCategoryIdByKeyword(merchant.lowercase())
            val confidence = merchantDao.getConfidenceByKeyword(merchant.lowercase()) ?: 0

            val categoryName = if (categoryId != null) {
                val category = db.categoryDao().getCategoryById(categoryId)
                category?.name ?: "Other"
            } else {
                "Other"
            }

            if (confidence > 80) {
                val transaction = Transaction(
                    title = merchant,
                    amount = parsedSms.amount!!,
                    category = categoryName,
                    timestamp = smsTimestamp,  // ← Use SMS timestamp!
                    source = "sms"
                )
                db.transactionDao().insertTransaction(transaction)

                NotificationHelper.showTransactionNotification(
                    context,
                    "Transaction Detected",
                    "₹${parsedSms.amount} - $merchant ($categoryName) [Auto-added]"
                )
            }
        }
    }
}