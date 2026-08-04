package com.roadscript.oss.ui.utils

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.FileProvider
import java.io.File

object FileHelper {

    private val blacklistedExtensions = listOf("apk", "exe", "bin", "sh", "bat", "msi", "com", "vbs")

    fun isBlacklisted(path: String): Boolean {
        val ext = path.substringAfterLast('.', "").lowercase()
        return ext in blacklistedExtensions
    }

    fun getIconForFile(path: String): ImageVector {
        val extension = path.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "pdf" -> Icons.Default.PictureAsPdf
            "jpg", "jpeg", "png", "gif", "webp", "bmp" -> Icons.Default.Image
            "txt", "log", "md" -> Icons.AutoMirrored.Filled.Article
            "xml", "html", "json", "js", "css", "py", "kt", "java" -> Icons.Default.Code
            "doc", "docx", "odt" -> Icons.Default.Description
            "xls", "xlsx", "ods", "csv" -> Icons.Default.TableChart
            else -> Icons.AutoMirrored.Filled.Article
        }
    }

    fun isImage(path: String): Boolean {
        val extension = path.substringAfterLast('.', "").lowercase()
        return extension in listOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
    }

    /**
     * Convertit un chemin absolu en chemin relatif par rapport à context.filesDir.
     */
    fun toRelativePath(context: Context, absolutePath: String): String {
        if (absolutePath.isBlank()) return ""
        val root = context.filesDir.absolutePath
        return if (absolutePath.startsWith(root)) {
            absolutePath.substring(root.length).removePrefix("/")
        } else absolutePath
    }

    /**
     * Résout un chemin relatif en chemin absolu par rapport à context.filesDir.
     * Si le chemin est déjà absolu, il est retourné tel quel.
     */
    fun toAbsolutePath(context: Context, path: String): String {
        if (path.isBlank() || path.startsWith("/")) return path
        return File(context.filesDir, path).absolutePath
    }

    fun openFile(context: Context, path: String) {
        val absolutePath = toAbsolutePath(context, path)
        val file = File(absolutePath)
        if (!file.exists()) {
            Toast.makeText(context, "Le fichier n'existe pas.", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val extension = file.extension.lowercase()
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "*/*"

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            // Forcer l'affichage du sélecteur d'applications (Chooser)
            val chooser = Intent.createChooser(intent, "Ouvrir avec...")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            
            context.startActivity(chooser)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "Aucune application trouvée pour ouvrir ce type de fichier ($path).", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Erreur lors de l'ouverture : ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name = "file_${System.currentTimeMillis()}"
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return sanitizeFilename(name)
    }

    fun getVehicleFolder(context: Context, vehicleId: String): File {
        val garageDir = File(context.filesDir, "garage")
        val vehicleDir = File(garageDir, vehicleId)
        if (!vehicleDir.exists()) vehicleDir.mkdirs()
        return vehicleDir
    }

    fun copyUriToInternal(context: Context, uri: Uri, targetFolder: File): String? {
        return try {
            val fileName = getFileNameFromUri(context, uri)
            if (!targetFolder.exists()) targetFolder.mkdirs()

            val uniqueFileName = "${System.currentTimeMillis()}_$fileName"
            val destFile = File(targetFolder, uniqueFileName)

            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            toRelativePath(context, destFile.absolutePath)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * @deprecated Use version with File targetFolder
     */
    fun copyUriToInternal(context: Context, uri: Uri, subfolder: String = "Attachments"): String? {
        val dir = File(context.filesDir, subfolder)
        return copyUriToInternal(context, uri, dir)
    }

    fun safeDelete(context: Context, path: String?) {
        if (path.isNullOrBlank()) return
        try {
            val absolutePath = toAbsolutePath(context, path)
            val file = File(absolutePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun sanitizeFilename(displayName: String): String {
        val badCharacters = arrayOf("..", "/")
        val segments = displayName.split("/")
        var fileName = segments.last()
        for (suspString in badCharacters) {
            fileName = fileName.replace(suspString, "_")
        }
        return fileName
    }
}
