package com.example.expensetracker

import org.junit.Test
import org.junit.Assert.*

class LocalRegexParserTest {

    @Test
    fun testParseStarbucksCardNotification() {
        val sms = "Spent Rs 350 at Starbucks Coffee on Card 1122"
        val parsed = LocalRegexParser.parseNotification(sms)
        assertNotNull(parsed)
        assertEquals(350, parsed!!.amount)
        assertEquals("Starbucks Coffee", parsed.title)
        assertEquals("Food", parsed.category)
        assertEquals("Card", parsed.paymentMethod)
    }

    @Test
    fun testParseSwiggyNotification() {
        val sms = "Rs. 1,500 debited from A/C for Swiggy payment"
        val parsed = LocalRegexParser.parseNotification(sms)
        assertNotNull(parsed)
        assertEquals(1500, parsed!!.amount)
        assertEquals("Food", parsed.category)
        assertEquals("Bank Transfer", parsed.paymentMethod)
    }

    @Test
    fun testParseOlaUpiNotification() {
        val sms = "Paid INR 200 to Ola Cabs via UPI"
        val parsed = LocalRegexParser.parseNotification(sms)
        assertNotNull(parsed)
        assertEquals(200, parsed!!.amount)
        assertEquals("Ola Cabs", parsed.title)
        assertEquals("Transport", parsed.category)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun testParseZaraShoppingNotification() {
        val sms = "Spent Rs 1200 at Zara via UPI"
        val parsed = LocalRegexParser.parseNotification(sms)
        assertNotNull(parsed)
        assertEquals(1200, parsed!!.amount)
        assertEquals("Zara", parsed.title)
        assertEquals("Shopping", parsed.category)
        assertEquals("UPI", parsed.paymentMethod)
    }

    @Test
    fun testParseElectricityBillNotification() {
        val sms = "Electricity bill of Rs 4500 paid on Credit Card"
        val parsed = LocalRegexParser.parseNotification(sms)
        assertNotNull(parsed)
        assertEquals(4500, parsed!!.amount)
        assertEquals("Bills", parsed.category)
        assertEquals("Card", parsed.paymentMethod)
    }

    @Test
    fun testParseNonTransactionText() {
        val sms = "Hey, let's meet up at 5 PM."
        val parsed = LocalRegexParser.parseNotification(sms)
        assertNull(parsed)
    }
}
