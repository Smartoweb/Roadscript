package com.roadscript.oss.ui.utils

import android.graphics.*
import java.text.SimpleDateFormat
import java.util.*

object ChartDrawer {

    /**
     * Dessine un graphique simplifié sur un Bitmap pour l'export PDF.
     */
    fun drawPriceChart(history: List<com.roadscript.oss.data.models.FuelReading>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        if (history.size < 2) return bitmap

        val paint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val pointPaint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val sorted = history.sortedBy { it.date }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
        
        val startTime = sdf.parse(sorted.first().date)?.time ?: 0L
        val endTime = sdf.parse(sorted.last().date)?.time ?: (startTime + 1)
        val timeRange = (endTime - startTime).toDouble().coerceAtLeast(1.0)

        val prices = sorted.map { if (it.liters > 0) it.cost / it.liters else 0.0 }
        val minPrice = (prices.minOrNull() ?: 0.0) * 0.98
        val maxPrice = (prices.maxOrNull() ?: 1.0) * 1.02
        val priceRange = (maxPrice - minPrice).coerceAtLeast(0.1)

        val paddingX = 60f
        val paddingTop = 60f
        val paddingBottom = 40f
        
        val innerWidth = width - 2 * paddingX
        val innerHeight = height - paddingTop - paddingBottom

        val path = Path()
        sorted.forEachIndexed { index, reading ->
            val time = sdf.parse(reading.date)?.time ?: startTime
            val price = if (reading.liters > 0) reading.cost / reading.liters else 0.0
            
            val x = paddingX + ((time - startTime) / timeRange * innerWidth).toFloat()
            val y = height - paddingBottom - ((price - minPrice) / priceRange * innerHeight).toFloat()
            
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            
            // Dessiner le point
            canvas.drawCircle(x, y, 5f, pointPaint)
            
            // Dessiner le label (Prix uniquement)
            val label = String.format(Locale.FRANCE, "%.3f", price)
            canvas.drawText(label, x, y - 12f, textPaint)
        }
        canvas.drawPath(path, paint)

        // Dessiner les dates de début et de fin sur l'axe X
        val axisDatePaint = Paint(textPaint).apply {
            color = Color.GRAY
            textSize = 12f
        }
        try {
            val startDate = formatDateShort(sorted.first().date)
            val endDate = formatDateShort(sorted.last().date)
            canvas.drawText(startDate, paddingX, height - paddingBottom + 20f, axisDatePaint)
            canvas.drawText(endDate, width - paddingX, height - paddingBottom + 20f, axisDatePaint)
        } catch (_: Exception) {}

        return bitmap
    }

    fun drawMileageChart(history: List<com.roadscript.oss.data.models.MileageReading>, width: Int, height: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        if (history.size < 2) return bitmap

        val paint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            strokeWidth = 3f
            style = Paint.Style.STROKE
            isAntiAlias = true
        }

        val pointPaint = Paint().apply {
            color = Color.parseColor("#3F51B5")
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 14f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val sorted = history.sortedBy { it.date }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
        val shortSdf = SimpleDateFormat("dd/MM", Locale.FRANCE)

        val startTime = sdf.parse(sorted.first().date)?.time ?: 0L
        val endTime = sdf.parse(sorted.last().date)?.time ?: (startTime + 1)
        val timeRange = (endTime - startTime).toDouble().coerceAtLeast(1.0)

        val minKm = sorted.minOf { it.value }.toDouble() * 0.99
        val maxKm = sorted.maxOf { it.value }.toDouble() * 1.01
        val kmRange = (maxKm - minKm).coerceAtLeast(10.0)

        val paddingX = 60f
        val paddingTop = 60f
        val paddingBottom = 40f
        
        val innerWidth = width - 2 * paddingX
        val innerHeight = height - paddingTop - paddingBottom

        val path = Path()
        sorted.forEachIndexed { index, reading ->
            val time = sdf.parse(reading.date)?.time ?: startTime
            val km = reading.value.toDouble()
            
            val x = paddingX + ((time - startTime) / timeRange * innerWidth).toFloat()
            val y = height - paddingBottom - ((km - minKm) / kmRange * innerHeight).toFloat()
            
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

            // Dessiner le point
            canvas.drawCircle(x, y, 5f, pointPaint)
            
            // Dessiner le label (KM uniquement)
            canvas.drawText("${reading.value}", x, y - 12f, textPaint)
        }
        canvas.drawPath(path, paint)

        // Dessiner les dates de début et de fin sur l'axe X
        val axisDatePaint = Paint(textPaint).apply {
            color = Color.GRAY
            textSize = 12f
        }
        try {
            val startDate = formatDateShort(sorted.first().date)
            val endDate = formatDateShort(sorted.last().date)
            canvas.drawText(startDate, paddingX, height - paddingBottom + 20f, axisDatePaint)
            canvas.drawText(endDate, width - paddingX, height - paddingBottom + 20f, axisDatePaint)
        } catch (_: Exception) {}

        return bitmap
    }

    private fun formatDateShort(dateStr: String): String {
        return try {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
            val formatter = SimpleDateFormat("dd/MM/yy", Locale.FRANCE)
            formatter.format(parser.parse(dateStr)!!)
        } catch (_: Exception) {
            dateStr
        }
    }
}
