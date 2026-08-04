package com.roadscript.oss.ui.utils

import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipHelper {

    /**
     * Crée un fichier ZIP contenant le fichier XML de données et une liste de fichiers ressources.
     * @param attachmentFiles Liste de paires (Fichier physique, Chemin relatif dans le ZIP)
     */
    fun createZip(
        outputStream: OutputStream,
        xmlFile: File,
        attachmentFiles: List<Pair<File, String>>
    ) {
        ZipOutputStream(BufferedOutputStream(outputStream)).use { zos ->
            // 1. Ajouter le fichier XML
            if (xmlFile.exists()) {
                addToZip(zos, xmlFile, "data.xml")
            }

            // 2. Ajouter les fichiers ressources sélectionnés
            attachmentFiles.forEach { (file, zipPath) ->
                if (file.exists() && file.isFile) {
                    addToZip(zos, file, zipPath)
                }
            }
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }

    /**
     * Extrait un fichier ZIP dans un dossier temporaire.
     * Retourne une paire contenant le fichier XML extrait et le dossier "garage" extrait (s'il existe).
     */
    fun extractZip(
        inputStream: InputStream,
        targetDir: File
    ): Pair<File?, File?> {
        if (!targetDir.exists()) targetDir.mkdirs()
        
        var extractedXml: File? = null
        var garageDir: File? = null

        ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            while (entry != null) {
                val file = File(targetDir, entry.name)
                
                if (entry.isDirectory) {
                    file.mkdirs()
                } else {
                    file.parentFile?.mkdirs()
                    FileOutputStream(file).use { fos ->
                        zis.copyTo(fos)
                    }
                    
                    if (entry.name == "data.xml") {
                        extractedXml = file
                    }
                }

                // Détecter le dossier garage (insensible à la casse pour plus de souplesse)
                if (entry.name.startsWith("garage/", ignoreCase = true) || entry.name.startsWith("Garage/", ignoreCase = true)) {
                    val rootFolderName = entry.name.substringBefore('/')
                    garageDir = File(targetDir, rootFolderName)
                }

                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return Pair(extractedXml, if (garageDir?.exists() == true) garageDir else null)
    }
}
