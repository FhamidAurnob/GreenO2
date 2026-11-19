package com.example.greeno2

import com.example.greeno2.domain.PasswordHasher
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PasswordHasherTest {
    @Test
    fun verifiesCorrectPasswordOnly() {
        val h = PasswordHasher.hash("green-miles-123")
        assertTrue(PasswordHasher.verify("green-miles-123", h.hash, h.salt))
        assertFalse(PasswordHasher.verify("wrong", h.hash, h.salt))
    }

    @Test
    fun samePasswordGetsDifferentSalts() {
        val a = PasswordHasher.hash("secret1")
        val b = PasswordHasher.hash("secret1")
        assertNotEquals(a.salt, b.salt)
        assertNotEquals(a.hash, b.hash)
    }
}
