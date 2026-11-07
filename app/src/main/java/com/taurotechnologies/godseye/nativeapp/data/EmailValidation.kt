package com.taurotechnologies.godseye.nativeapp.data

/** Lightweight client-side check; Supabase remains authoritative for account validation. */
fun isValidEmail(value: String): Boolean {
    val email = value.trim()
    if (email.isEmpty() || email.any(Char::isWhitespace)) return false
    val at = email.indexOf('@')
    if (at <= 0 || at != email.lastIndexOf('@')) return false
    val local = email.substring(0, at)
    val domain = email.substring(at + 1)
    return local.isNotEmpty() && !local.startsWith('.') && !local.endsWith('.') &&
        !local.contains("..") && domain.contains('.') && !domain.startsWith('.') &&
        !domain.endsWith('.') && domain.split('.').all { it.isNotEmpty() }
}
