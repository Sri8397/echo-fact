package com.taurotechnologies.godseye.nativeapp.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CountryCentroidsTest {
    @Test
    fun lookupIsCaseInsensitiveAndRejectsUnknownOrBlankCodes() {
        assertEquals("Pakistan", countryCentre("pk")?.name)
        assertEquals("Pakistan", countryCentre(" PK ")?.name)
        assertNull(countryCentre("XX"))
        assertNull(countryCentre(""))
        assertNull(countryCentre(null))
    }

    @Test
    fun everyCentreIsAValidCoordinateInsideItsCountryRange() {
        assertTrue(COUNTRY_CENTRES.size > 150)
        for ((code, centre) in COUNTRY_CENTRES) {
            assertTrue("$code latitude", centre.lat in -90.0..90.0)
            assertTrue("$code longitude", centre.lng in -180.0..180.0)
            assertTrue("$code is not null island", !(centre.lat == 0.0 && centre.lng == 0.0))
            assertTrue("$code key is ISO alpha-2", code.matches(Regex("[A-Z]{2}")))
        }
    }

    @Test
    fun centresForCountriesUsedByLiveLayersAreInTheRightPlace() {
        val russia = countryCentre("RU")!!
        assertTrue(russia.lng > 60 && russia.lat > 50)
        val sudan = countryCentre("SD")!!
        assertTrue(sudan.lat in 10.0..20.0 && sudan.lng in 20.0..35.0)
    }
}
