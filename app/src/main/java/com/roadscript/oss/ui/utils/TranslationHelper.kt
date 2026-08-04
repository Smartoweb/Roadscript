package com.roadscript.oss.ui.utils

import android.content.Context
import android.content.res.XmlResourceParser
import com.roadscript.oss.R

object TranslationHelper {

    fun getCategoryName(context: Context, key: String, defaultName: String): String {
        return when (key) {
            "entretien" -> context.getString(R.string.cat_service)
            "depannage" -> context.getString(R.string.cat_repair)
            "obligation" -> context.getString(R.string.cat_legal)
            "accessoire" -> context.getString(R.string.cat_accessory)
            else -> defaultName
        }
    }

    fun getCategoryDescription(context: Context, key: String, defaultDesc: String): String {
        return when (key) {
            "entretien" -> context.getString(R.string.cat_service_desc)
            "depannage" -> context.getString(R.string.cat_repair_desc)
            "obligation" -> context.getString(R.string.cat_legal_desc)
            "accessoire" -> context.getString(R.string.cat_accessory_desc)
            else -> defaultDesc
        }
    }

    fun getSubCategoryName(context: Context, key: String, defaultName: String): String {
        // Si l'utilisateur a saisi un nom personnalisé (différent de la clé technique), on le garde.
        if (defaultName.isNotBlank() && defaultName != key) {
            return defaultName
        }

        return when {
            key.contains("carrosserie") -> context.getString(R.string.sub_body)
            key.contains("climatisation") || key.contains("clim") -> context.getString(R.string.sub_air_con)
            key.contains("electricite") || key.contains("elec") -> context.getString(R.string.sub_electric)
            key.contains("freinage") || key.contains("frein") -> context.getString(R.string.sub_brakes)
            key.contains("moteur") -> context.getString(R.string.sub_engine)
            key.contains("pneu") -> context.getString(R.string.sub_tires)
            key.contains("signalisation") || key.contains("signa") -> context.getString(R.string.sub_lights)
            key.contains("suspension") -> context.getString(R.string.sub_suspension)
            key.contains("transmission") -> context.getString(R.string.sub_transmission)
            key.contains("main_d_oeuvre") -> context.getString(R.string.sub_labor)
            key.contains("consommables") -> context.getString(R.string.sub_consumables)
            key.contains("autre") -> context.getString(R.string.sub_other)
            
            key == "obl_contr_le_technique" -> context.getString(R.string.sub_inspection)
            key == "obl_contre_visite_ct" -> context.getString(R.string.sub_reinspection)
            key == "obl_assurance" -> context.getString(R.string.sub_insurance)
            
            key == "acc_audio" -> context.getString(R.string.sub_audio)
            key == "acc_confort" -> context.getString(R.string.sub_comfort)
            key == "acc_esth_tique_nettoyage" -> context.getString(R.string.sub_aesthetics)
            key == "acc_quipement" -> context.getString(R.string.sub_equipment)
            
            else -> defaultName
        }
    }

    fun getDecoratedSubCategoryName(
        context: Context, 
        key: String, 
        defaultName: String, 
        currentCountryCode: String,
        itemCountryCode: String? = null
    ): String {
        var baseName = getSubCategoryName(context, key, defaultName)
        
        if (baseName == key && key.length >= 3 && key[2] == '_') {
            baseName = getNameFromJurisdictions(context, key) ?: baseName
        }
        
        return baseName
    }

    private fun getNameFromJurisdictions(context: Context, key: String): String? {
        try {
            val parser = context.resources.getXml(R.xml.jurisdictions)
            var eventType = parser.eventType
            // On récupère la langue actuellement appliquée au contexte (qui peut être différente du système)
            val currentLang = context.resources.configuration.locales[0].language
            
            while (eventType != XmlResourceParser.END_DOCUMENT) {
                if (eventType == XmlResourceParser.START_TAG && parser.name == "obligation") {
                    val id = parser.getAttributeValue(null, "id")
                    if (id == key) {
                        return when (currentLang) {
                            "fr" -> parser.getAttributeValue(null, "fr")
                            "es" -> parser.getAttributeValue(null, "es")
                            "de" -> parser.getAttributeValue(null, "de")
                            "it" -> parser.getAttributeValue(null, "it")
                            "pt" -> parser.getAttributeValue(null, "pt")
                            else -> parser.getAttributeValue(null, "en")
                        } ?: parser.getAttributeValue(null, "en") ?: parser.getAttributeValue(null, "fr")
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
