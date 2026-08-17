package com.example.pizzaconfigurator.kitchen.data.dto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OrderDtosTest {

    @Test
    fun `next action follows the CONFIRMED to READY state machine`() {
        assertEquals("approve", nextActionFor("CONFIRMED"))
        assertEquals("start", nextActionFor("APPROVED"))
        assertEquals("ready", nextActionFor("IN_PROCESSING"))
        assertEquals("complete", nextActionFor("READY"))
    }

    @Test
    fun `terminal statuses have no next action`() {
        assertNull(nextActionFor("COMPLETED"))
        assertNull(nextActionFor("CANCELLED"))
        assertNull(nextActionFor("REJECTED"))
    }
}
