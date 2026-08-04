package com.roadscript.oss.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.documentfile.provider.DocumentFile
import com.roadscript.oss.R
import com.roadscript.oss.data.models.*
import com.roadscript.oss.ui.utils.TranslationHelper
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object ExportManager {

    /**
     * Exportation directe vers PDF + Documents Originaux.
     */
    fun startExport(
        context: Context,
        targetTreeUri: Uri,
        vehicle: Vehicle?,
        mileage: List<MileageReading>,
        fuel: List<FuelReading>,
        events: List<Evenement>,
        categories: List<Category>,
        subCategories: List<SubCategory>,
        options: ExportOptions
    ) {
        val appName = "Roadscript"
        
        // Affichage immédiat du message de patience
        android.widget.Toast.makeText(context, context.getString(R.string.msg_please_wait), android.widget.Toast.LENGTH_SHORT).show()

        // Pré-configuration globale WebView
        try {
            WebView.enableSlowWholeDocumentDraw()
        } catch (e: Exception) {}

        val rootDir = DocumentFile.fromTreeUri(context, targetTreeUri) ?: return
        
        // 1. Dossier des originaux avec timestamp AAMMJJHHMM
        val timestamp = SimpleDateFormat("yyMMddHHmm", Locale.getDefault()).format(Date())
        val folderName = "${appName.lowercase()}_infos_$timestamp"
        val docsDir = rootDir.createDirectory(folderName) ?: rootDir
        
        val filesToCopy = mutableListOf<Attachment>()
        
        // Collecte des documents (Profil, Assurance, Garage)
        if (!options.isThirdParty) {
            vehicle?.photoPath?.let { if (it.isNotBlank()) filesToCopy.add(Attachment(it, "Photo Profil")) }
            if (options.includeAssurance) {
                vehicle?.insurance?.logoPath?.let { if (it.isNotBlank()) filesToCopy.add(Attachment(it, "Logo Assurance")) }
                filesToCopy.addAll(vehicle?.insurance?.photos ?: emptyList())
            }
            if (options.includeMechanic) {
                vehicle?.mechanic?.logoPath?.let { if (it.isNotBlank()) filesToCopy.add(Attachment(it, "Logo Garage")) }
                filesToCopy.addAll(vehicle?.mechanic?.photos ?: emptyList())
            }
            // Documents généraux du véhicule
            filesToCopy.addAll(vehicle?.photos ?: emptyList())
        }

        // Collecte des documents d'événements
        if (options.includeEvents) {
            events.forEach { evt ->
                // Pour un tiers, on filtre les obligations
                if (!options.isThirdParty || evt.categoryKey != "obligation") {
                    filesToCopy.addAll(evt.photos)
                }
            }
        }
        
        // Copie physique des fichiers originaux
        filesToCopy.distinctBy { it.path }.forEach { attachment ->
            copyLocalFileToSaf(context, attachment.path, docsDir)
        }

        // 2. Préparation du fichier PDF avec le même nom
        val pdfFileName = "$folderName.pdf"
        val pdfFile = rootDir.createFile("application/pdf", pdfFileName) ?: return
        
        val html = generateReportHtml(context, vehicle, mileage, fuel, events, categories, subCategories, options)
        
        // 3. Rendu PDF Silencieux (sur le thread UI)
        Handler(Looper.getMainLooper()).post {
            renderHtmlToPdfSilently(context, html, pdfFile.uri)
        }
    }

    private fun copyLocalFileToSaf(context: Context, localPath: String, targetDir: DocumentFile) {
        val absolutePath = FileHelper.toAbsolutePath(context, localPath)
        val sourceFile = File(absolutePath)
        if (!sourceFile.exists()) return
        val newFile = targetDir.createFile("application/octet-stream", sourceFile.name) ?: return
        try {
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                sourceFile.inputStream().use { input -> input.copyTo(output) }
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun renderHtmlToPdfSilently(context: Context, html: String, targetUri: Uri) {
        val webView = WebView(context)
        webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        
        webView.settings.apply {
            javaScriptEnabled = true
            defaultTextEncodingName = "UTF-8"
            textZoom = 100
            useWideViewPort = true
            loadWithOverviewMode = false
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                Handler(Looper.getMainLooper()).postDelayed({
                    try {
                        val renderWidth = 1050
                        webView.measure(
                            View.MeasureSpec.makeMeasureSpec(renderWidth, View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                        )
                        val width = webView.measuredWidth
                        val height = webView.measuredHeight
                        webView.layout(0, 0, width, height)
                        
                        finalizePdf(context, webView, targetUri)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, 3000)
            }
        }
        
        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
    }

    private fun finalizePdf(context: Context, webView: WebView, targetUri: Uri) {
        try {
            val width = webView.measuredWidth
            val height = webView.measuredHeight
            if (height <= 0 || width <= 0) return

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            webView.draw(canvas)

            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)

            context.contentResolver.openOutputStream(targetUri)?.use { output ->
                pdfDocument.writeTo(output)
            }
            pdfDocument.close()
            bitmap.recycle()

            android.widget.Toast.makeText(context, context.getString(R.string.msg_operation_finished), android.widget.Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            e.printStackTrace()
            android.widget.Toast.makeText(context, "${context.getString(R.string.msg_error)}: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateReportHtml(
        context: Context,
        vehicle: Vehicle?,
        mileage: List<MileageReading>,
        fuel: List<FuelReading>,
        events: List<Evenement>,
        categories: List<Category>,
        subCategories: List<SubCategory>,
        options: ExportOptions
    ): String {
        val appName = "Roadscript"
        val currentCountryCode = vehicle?.countryCode ?: ""
        val sb = StringBuilder()
        sb.append("<html><head><meta charset='UTF-8'><meta name='viewport' content='width=1050'>")
        sb.append("<style>")
        sb.append("""
            * { box-sizing: border-box; }
            body { font-family: sans-serif; color: #333; margin: 0; padding: 50px; background: white; width: 1050px; }
            h1 { color: #3F51B5; border-bottom: 3px solid #3F51B5; padding-bottom: 12px; font-size: 28px; margin-top: 0; width: 100%; word-wrap: break-word; }
            h2 { color: #5C6BC0; margin-top: 40px; border-left: 6px solid #5C6BC0; padding-left: 15px; font-size: 22px; }
            table { width: 100%; border-collapse: collapse; margin-top: 20px; table-layout: auto; }
            th, td { border: 1px solid #bbb; padding: 14px 10px; text-align: left; font-size: 16px; word-wrap: break-word; vertical-align: top; }
            th { background-color: #f0f0f0; font-weight: bold; width: 30%; }
            img { max-width: 100%; height: auto; display: block; margin: 15px 0; border: 1px solid #eee; }
            .badge { background: #E8EAF6; padding: 3px 8px; border-radius: 4px; font-size: 13px; }
            .cost { font-weight: bold; text-align: right; min-width: 120px; }
            .gallery { display: block; width: 100%; margin-top: 30px; }
            .gallery-item { display: inline-block; width: 31%; border: 1px solid #ddd; padding: 8px; margin: 1%; text-align: center; vertical-align: top; background: #fff; }
            .gallery-img { width: 100%; height: 160px; object-fit: contain; }
            .filename { font-size: 11px; color: #555; display: block; margin-top: 8px; overflow-wrap: break-word; }
        """.trimIndent())
        sb.append("</style></head><body>")

        // Titre
        sb.append("<h1>${context.getString(R.string.pdf_report_title, appName)}</h1>")
        sb.append("<h3>${vehicle?.brand} ${vehicle?.model}</h3>")
        val formattedDate = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        sb.append("<p style='font-size:10px;'>${context.getString(R.string.pdf_generated_on, formattedDate)}</p>")

        // Infos Véhicule
        sb.append("<h2>${context.getString(R.string.pdf_section_veh)}</h2>")
        sb.append("<table>")
        sb.append("<tr><th>${context.getString(R.string.pdf_label_model)}</th><td>${vehicle?.brand} ${vehicle?.model}</td></tr>")
        sb.append("<tr><th>${context.getString(R.string.pdf_label_plate)}</th><td>${vehicle?.plateNumber}</td></tr>")
        sb.append("<tr><th>${context.getString(R.string.pdf_label_energy)}</th><td>${vehicle?.fuelType}</td></tr>")
        
        val countryName = if (currentCountryCode.isBlank()) "" else java.util.Locale("", currentCountryCode).getDisplayCountry(Locale.getDefault())
        if (countryName.isNotBlank()) {
            sb.append("<tr><th>${context.getString(R.string.settings_country)}</th><td>$countryName</td></tr>")
        }
        
        sb.append("</table>")

        // Assurance / Garage
        if (options.includeAssurance && !vehicle?.insurance?.name.isNullOrBlank()) {
            val contractStr = if (options.isThirdParty) "..." else vehicle?.insurance?.contractNumber
            sb.append("<h2>${context.getString(R.string.pdf_section_insur)}</h2><p style='font-size:12px;'><strong>${vehicle?.insurance?.name}</strong> - ${context.getString(R.string.pdf_label_contract)} : $contractStr</p>")
        }
        if (options.includeMechanic && !vehicle?.mechanic?.name.isNullOrBlank()) {
            val custStr = if (options.isThirdParty) "..." else vehicle?.mechanic?.customerNumber
            sb.append("<h2>${context.getString(R.string.pdf_section_mech)}</h2><p style='font-size:12px;'><strong>${vehicle?.mechanic?.name}</strong> - ${vehicle?.mechanic?.address} - No: $custStr</p>")
        }

        // KM
        if (options.mileageOption >= 0) {
            sb.append("<h2>${context.getString(R.string.pdf_section_mileage)}</h2>")
            val latest = mileage.firstOrNull()
            if (latest != null) sb.append("<p style='font-size:12px;'>${context.getString(R.string.pdf_label_last_reading)} : <strong>${latest.value} km</strong> ${context.getString(R.string.pdf_on)} ${formatDate(latest.date)}</p>")
            if (options.mileageOption == 1 && mileage.size >= 2) {
                val chartBmp = ChartDrawer.drawMileageChart(mileage, 800, 300)
                sb.append("<img src='data:image/png;base64,${bitmapToBase64(chartBmp)}' style='width:100%; height:auto; border:1px solid #eee;' />")
            }
        }

        // Carburant
        if (options.fuelOption >= 1) {
            sb.append("<h2>${context.getString(R.string.pdf_section_fuel)}</h2>")
            val totalLiters = fuel.sumOf { it.liters }
            sb.append("<p style='font-size:12px;'>${context.getString(R.string.pdf_label_vol_total)} : ${String.format(Locale.getDefault(), "%,.1f L", totalLiters)}</p>")
            if (options.fuelOption == 2 && fuel.size >= 2) {
                val chartBmp = ChartDrawer.drawPriceChart(fuel, 800, 300)
                sb.append("<img src='data:image/png;base64,${bitmapToBase64(chartBmp)}' style='width:100%; height:auto; border:1px solid #eee;' />")
            }
        }

        // Interventions
        if (options.includeEvents && events.isNotEmpty()) {
            sb.append("<h2>${context.getString(R.string.pdf_section_history)}</h2>")
            sb.append("<table><tr><th>${context.getString(R.string.pdf_col_date)}</th><th>${context.getString(R.string.pdf_col_action)}</th><th>${context.getString(R.string.pdf_col_notes)}</th><th>${context.getString(R.string.pdf_col_cost)}</th></tr>")
            events.forEach { evt ->
                val category = categories.find { it.key == evt.categoryKey }
                val catName = if (category != null) {
                    TranslationHelper.getCategoryName(context, category.key, category.name)
                } else evt.categoryKey

                sb.append("<tr><td>${formatDate(evt.date)}</td><td><strong>${evt.name}</strong><br/><small>$catName</small></td>")
                
                val detail = if (evt.isMultiple) {
                    evt.actions.joinToString("<br/>") { action ->
                        val translatedSub = TranslationHelper.getDecoratedSubCategoryName(context, action.subCategoryKey, action.name, currentCountryCode, action.countryCode ?: evt.countryCode)
                        "- $translatedSub"
                    }
                } else {
                    val sub = subCategories.find { it.key == evt.subCategoryKey }
                    val translatedSub = TranslationHelper.getDecoratedSubCategoryName(context, evt.subCategoryKey, sub?.name ?: evt.subCategoryKey, currentCountryCode, evt.countryCode)
                    if (translatedSub.startsWith("(")) {
                        "<strong>$translatedSub</strong><br/>${evt.description}"
                    } else {
                        evt.description
                    }
                }
                val currency = CountryHelper.getCurrencySymbol(evt.countryCode ?: "")
                sb.append("<td>$detail</td><td class='cost'>${String.format(Locale.getDefault(), "%.2f $currency", evt.cost)}</td></tr>")
            }
            sb.append("</table>")
        }

        // Annexes
        if (options.includeAttachments) {
            val allDocs = mutableListOf<Attachment>()
            
            if (options.isThirdParty) {
                // Tiers : Uniquement les justificatifs d'entretien, dépannage et accessoires (si events inclus)
                if (options.includeEvents) {
                    events.filter { it.categoryKey != "obligation" }.forEach { allDocs.addAll(it.photos) }
                }
            } else {
                // Personnel : Tout ce qui est coché + les documents du véhicule
                if (options.includeAssurance) vehicle?.insurance?.photos?.let { allDocs.addAll(it) }
                if (options.includeMechanic) vehicle?.mechanic?.photos?.let { allDocs.addAll(it) }
                if (options.includeEvents) events.forEach { evt -> allDocs.addAll(evt.photos) }
                vehicle?.photos?.let { allDocs.addAll(it) }
            }
            
            val distinctAttachments = allDocs.distinctBy { it.path }
            if (distinctAttachments.isNotEmpty()) {
                sb.append("<div style='page-break-before: always;'><h2>${context.getString(R.string.pdf_section_annexes)}</h2><div class='gallery'>")
                distinctAttachments.forEach { attachment ->
                    val absolutePath = FileHelper.toAbsolutePath(context, attachment.path)
                    val file = File(absolutePath)
                    if (file.exists()) {
                        sb.append("<div class='gallery-item'>")
                        if (isImage(attachment.path)) {
                            sb.append("<img class='gallery-img' src='data:image/jpeg;base64,${getFileAsBase64(context, attachment.path, 300)}' />")
                        } else if (attachment.path.lowercase().endsWith(".pdf")) {
                            sb.append("<img class='gallery-img' src='data:image/png;base64,${getPdfFirstPageAsBase64(context, attachment.path, 300)}' />")
                        } else {
                            sb.append("<div style='height:80px; background:#eee;'>DOC</div>")
                        }
                        val labelToDisplay = attachment.label.ifBlank { file.name }
                        sb.append("<span class='filename'><strong>$labelToDisplay</strong><br/><small>${file.name}</small></span></div>")
                    }
                }
                sb.append("</div></div>")
            }
        }

        sb.append("</body></html>")
        return sb.toString()
    }

    private fun formatDate(date: String?): String {
        if (date.isNullOrBlank()) return ""
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            formatter.format(parser.parse(date)!!)
        } catch (e: Exception) { date }
    }

    private fun isImage(path: String): Boolean {
        val ext = path.substringAfterLast('.').lowercase()
        return ext in listOf("jpg", "jpeg", "png", "webp")
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        val res = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        bitmap.recycle()
        return res
    }

    private fun getFileAsBase64(context: Context, path: String, maxDimension: Int): String {
        return try {
            val absolutePath = FileHelper.toAbsolutePath(context, path)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(absolutePath, options)
            var scale = 1
            while (options.outWidth / scale / 2 >= maxDimension && options.outHeight / scale / 2 >= maxDimension) scale *= 2
            val bitmap = BitmapFactory.decodeFile(absolutePath, BitmapFactory.Options().apply { inSampleSize = scale }) ?: return ""
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, out)
            val res = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            bitmap.recycle()
            res
        } catch (e: Exception) { "" }
    }

    private fun getPdfFirstPageAsBase64(context: Context, path: String, maxDimension: Int): String {
        return try {
            val absolutePath = FileHelper.toAbsolutePath(context, path)
            val file = File(absolutePath)
            val fd = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = android.graphics.pdf.PdfRenderer(fd)
            val page = renderer.openPage(0)
            val scale = maxDimension.toFloat() / page.width.coerceAtLeast(page.height)
            val bitmap = Bitmap.createBitmap((page.width * scale).toInt(), (page.height * scale).toInt(), Bitmap.Config.ARGB_8888)
            page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close(); renderer.close(); fd.close()
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            val res = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            bitmap.recycle()
            res
        } catch (e: Exception) { "" }
    }
}

data class ExportOptions(
    val includeAssurance: Boolean,
    val includeMechanic: Boolean,
    val mileageOption: Int,
    val fuelOption: Int,
    val includeEvents: Boolean,
    val includeAttachments: Boolean,
    val includeStats: Boolean,
    val isThirdParty: Boolean = false
)
