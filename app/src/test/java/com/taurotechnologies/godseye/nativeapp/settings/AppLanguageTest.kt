package com.taurotechnologies.godseye.nativeapp.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {
    @Test
    fun systemAndBlankFollowTheDevice() {
        assertNull(appLocaleFor("system"))
        assertNull(appLocaleFor(""))
    }

    @Test
    fun webCodesMapToLocales() {
        assertEquals("pt", appLocaleFor("pt")!!.language)
        assertEquals("fa", appLocaleFor("fa")!!.language)
        val traditional = appLocaleFor("zh-TW")!!
        assertEquals("zh", traditional.language)
        assertEquals("TW", traditional.country)
    }
}
