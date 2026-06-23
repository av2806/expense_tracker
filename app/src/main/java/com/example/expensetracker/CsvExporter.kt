package com.example.expensetracker

import java.io.BufferedWriter
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CsvExporter {

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    fun escapeCsvField(field: String): String {
        if (field.contains(",") || field.contains("\"") || field.contains("\n") || field.contains("\r")) {
            val escaped = field.replace("\"", "\"\"")
            return "\"$escaped\""
        }
        return field
    }

    fun exportTransactions(transactions: List<Transaction>, outputStream: OutputStream): Boolean {
        return try {
            BufferedWriter(OutputStreamWriter(outputStream)).use { writer ->
                // Write Header
                writer.write("ID,Title,Amount,Category,Date,Payment Method,Source,Phone Number,Bank Name,Bank Last 4\n")
                // Write Data
                transactions.forEach { tx ->
                    val id = tx.id
                    val title = escapeCsvField(tx.title)
                    val amount = tx.amount
                    val category = escapeCsvField(tx.category)
                    val date = escapeCsvField(formatDate(tx.timestamp))
                    val paymentMethod = escapeCsvField(tx.paymentMethod)
                    val source = escapeCsvField(tx.source)
                    val phoneNumber = escapeCsvField(tx.phoneNumber)
                    val bankName = escapeCsvField(tx.bankName)
                    val bankLast4 = escapeCsvField(tx.bankLast4)
                    
                    writer.write("$id,$title,$amount,$category,$date,$paymentMethod,$source,$phoneNumber,$bankName,$bankLast4\n")
                }
                writer.flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
