package com.taurotechnologies.godseye.nativeapp.settings

import android.content.Context
import android.content.res.Configuration
import com.taurotechnologies.godseye.nativeapp.data.LANGUAGE_SYSTEM
import com.taurotechnologies.godseye.nativeapp.data.NativePreferencesStore
import java.util.Locale

/** Locale for a stored language code, or null when the app should follow the device language. */
fun appLocaleFor(code: String): Locale? {
    if (code.isBlank() || code == LANGUAGE_SYSTEM) return null
    return Locale.forLanguageTag(code).takeIf { it.language.isNotEmpty() }
}

/**
 * Wraps an Activity base context so its resources load in the stored app language.
 * Call it from `attachBaseContext`, before any view or string is read. Returns [base] unchanged
 * when the language follows the device.
 */
fun withAppLanguage(base: Context): Context {
    val locale = appLocaleFor(NativePreferencesStore.peek(base).appLanguage) ?: return base
    val configuration = Configuration(base.resources.configuration)
    configuration.setLocale(locale)
    return base.createConfigurationContext(configuration)
}
