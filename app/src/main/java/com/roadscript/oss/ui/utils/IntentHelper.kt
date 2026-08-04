package com.roadscript.oss.ui.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object IntentHelper {

    fun dialNumber(context: Context, phone: String) {
        if (phone.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$phone")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Impossible de lancer l'appel : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(context: Context, email: String) {
        if (email.isBlank()) return
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$email")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Aucune application de messagerie trouvée.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWeb(context: Context, url: String) {
        if (url.isBlank()) return
        try {
            var formattedUrl = url.trim()
            if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                formattedUrl = "https://$formattedUrl"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Lien invalide ou aucun navigateur trouvé.", Toast.LENGTH_SHORT).show()
        }
    }
}
