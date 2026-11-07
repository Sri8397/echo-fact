package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmailValidationTest {
    @Test
    fun acceptsSimpleEmailAddressesAfterTrimming() {
        assertTrue(isValidEmail(" radar@example.com "))
        assertTrue(isValidEmail("first.last+radar@sub.example.org"))
    }

    @Test
    fun rejectsBlankAndMalformedEmailAddresses() {
        listOf("", "   ", "radar", "@example.com", "radar@", "radar@example", "radar@@example.com", "radar..name@example.com", "radar @example.com")
            .forEach { assertFalse("Expected invalid: $it", isValidEmail(it)) }
    }
}
