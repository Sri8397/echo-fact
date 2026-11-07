package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun parsesTokenFieldsAndPrefersServerExpiry() {
        val session = parseAuthResponseFields(
            accessToken = "access",
            refreshToken = "refresh",
            expiresAtEpochSeconds = 1_800_000_123L,
            expiresInSeconds = 3_600L,
            email = "user@example.com",
            userId = "user-id",
            nowEpochSeconds = 1_800_000_000L,
        )
        assertEquals("user@example.com", session?.email)
        assertEquals("user-id", session?.userId)
        assertEquals(1_800_000_123L, session?.expiresAtEpochSeconds)
    }

    @Test
    fun computesExpiryWhenServerOnlyReturnsDuration() {
        val session = parseAuthResponseFields("a", "r", null, 900, "", "", nowEpochSeconds = 100)
        assertEquals(1_000L, session?.expiresAtEpochSeconds)
    }

    @Test
    fun rejectsResponsesWithoutBothTokens() {
        assertNull(parseAuthResponseFields(null, "refresh", null, 3600, "", "", nowEpochSeconds = 100))
        assertNull(parseAuthResponseFields("access", " ", null, 3600, "", "", nowEpochSeconds = 100))
    }
}
