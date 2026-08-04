package com.roadscript.oss.ui.utils

object CountryHelper {
    val countryCodes = listOf(
        "DZ", "DE", "AR", "AU", "AT", "BE", "BR", "BG", "CA", "CL",
        "CN", "CO", "KR", "CI", "HR", "DK", "EG", "AE", "ES", "EE",
        "US", "FI", "FR", "GR", "HU", "IN", "ID", "IE", "IS", "IL",
        "IT", "JP", "LV", "LT", "LU", "MA", "MX", "MC", "NO", "NZ",
        "NL", "PE", "PL", "PT", "QA", "CZ", "RO", "GB", "RU", "SA",
        "SN", "SK", "SI", "SE", "CH", "TH", "TN", "TR", "UA", "VN"
    )

    /**
     * Retourne le symbole monétaire associé au code pays.
     * Utilise java.util.Currency pour détecter la monnaie locale.
     */
    fun getCurrencySymbol(countryCode: String): String {
        if (countryCode.isBlank()) return "€" // Fallback par défaut
        return try {
            val locale = java.util.Locale("", countryCode)
            java.util.Currency.getInstance(locale).getSymbol(java.util.Locale.getDefault())
        } catch (e: Exception) {
            "€"
        }
    }

    /**
     * Retourne le code pays par défaut (système si supporté, sinon le premier de la liste).
     */
    fun getDefaultCountryCode(): String {
        val locale = java.util.Locale.getDefault()
        val systemCountry = locale.country.uppercase()
        
        if (systemCountry.isNotBlank() && countryCodes.contains(systemCountry)) {
            return systemCountry
        }
        
        // Fallback basé sur la langue si le pays n'est pas précisé
        return when (locale.language.lowercase()) {
            "fr" -> "FR"
            "de" -> "DE"
            "es" -> "ES"
            "it" -> "IT"
            "pt" -> "PT"
            "en" -> "GB" // ou US, on choisit GB par défaut pour l'Europe
            else -> countryCodes.first()
        }
    }
}
