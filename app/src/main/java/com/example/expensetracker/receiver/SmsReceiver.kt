package com.example.expensetracker.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.expensetracker.LogManager
import com.example.expensetracker.SmsParser
import com.example.expensetracker.AppDatabase
import com.example.expensetracker.Transaction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {

        if (
            intent.action ==
            Telephony.Sms.Intents.SMS_RECEIVED_ACTION
        ) {

            val messages =
                Telephony.Sms.Intents
                    .getMessagesFromIntent(intent)

            for (sms in messages) {

                val body = sms.messageBody

                LogManager.log(
                    "SMS",
                    "Received: $body"
                )

                val parsed =
                    SmsParser.parseSms(body)

                if (parsed.isValidTransaction) {

                    LogManager.log(
                        "SMS",
                        "Transaction detected"
                    )

                    val db = AppDatabase.getDatabase(context)

                    CoroutineScope(Dispatchers.IO).launch {

                        db.transactionDao().insertTransaction(
                            Transaction(
                                title = parsed.merchant ?: "Unknown Merchant",
                                amount = parsed.amount ?: 0,
                                category = "Other",
                                paymentMethod = "SMS",
                                source = "sms"
                            )
                        )
                    }
                }
            }
        }
    }
}