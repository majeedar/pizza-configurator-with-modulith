package com.example.pizzaconfigurator.customer.data.dto

import java.math.BigDecimal
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class BigDecimalSerializerTest {

    @Serializable
    data class Money(@Serializable(with = BigDecimalSerializer::class) val amount: BigDecimal)

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `decodes a bare JSON number without losing trailing-zero precision`() {
        val money = json.decodeFromString(Money.serializer(), """{"amount":12.10}""")
        assertEquals(BigDecimal("12.10"), money.amount)
        assertEquals("12.10", money.amount.toPlainString())
    }

    @Test
    fun `round-trips through encode and decode`() {
        val original = Money(BigDecimal("9.99"))
        val encoded = json.encodeToString(Money.serializer(), original)
        val decoded = json.decodeFromString(Money.serializer(), encoded)
        assertEquals(original.amount, decoded.amount)
    }
}
