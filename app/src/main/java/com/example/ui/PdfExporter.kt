package com.example.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.data.ContentBlock
import com.example.data.Document
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    // A4 Dimensions in PostScript Points (1/72 inch)
    // A4 = 210mm x 297mm ~= 8.27in x 11.69in
    // Width = 8.27 * 72 = 595 pt
    // Height = 11.69 * 72 = 842 pt
    private const val A4_WIDTH = 595
    private const val A4_HEIGHT = 842
    private const val MARGIN = 54f // 0.75 in margin

    fun exportToPdf(context: Context, document: Document, blocks: List<ContentBlock>): File? {
        val pdfDocument = PdfDocument()
        
        // Setup paints
        val titlePaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 24f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val heading1Paint = TextPaint().apply {
            color = Color.rgb(33, 33, 33)
            textSize = 20f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val heading2Paint = TextPaint().apply {
            color = Color.rgb(66, 66, 66)
            textSize = 16f
            isAntiAlias = true
            isFakeBoldText = true
        }

        val bodyPaint = TextPaint().apply {
            color = Color.rgb(50, 50, 50)
            textSize = 12f
            isAntiAlias = true
        }

        val bulletPaint = TextPaint().apply {
            color = Color.rgb(50, 50, 50)
            textSize = 12f
            isAntiAlias = true
        }

        val footerPaint = Paint().apply {
            color = Color.GRAY
            textSize = 9f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        val printableWidth = (A4_WIDTH - 2 * MARGIN).toInt()
        var pageNumber = 1
        
        // Start first page
        var pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
        var currentPage = pdfDocument.startPage(pageInfo)
        var canvas = currentPage.canvas
        
        // Title banner
        var yPosition = MARGIN + 20f
        
        // Draw Document Title
        val titleLayout = StaticLayout.Builder.obtain(document.title, 0, document.title.length, titlePaint, printableWidth)
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.15f)
            .build()
            
        canvas.save()
        canvas.translate(MARGIN, yPosition)
        titleLayout.draw(canvas)
        canvas.restore()
        yPosition += titleLayout.height + 15f
        
        // Draw Accent Divider Line
        canvas.drawLine(MARGIN, yPosition, A4_WIDTH - MARGIN, yPosition, linePaint)
        yPosition += 25f

        // Draw blocks sequentially
        for (block in blocks) {
            val paintToUse = when (block.type) {
                "HEADING_1" -> heading1Paint
                "HEADING_2" -> heading2Paint
                "BULLET_LIST", "PARAGRAPH", "ALIGN_LEFT", "ALIGN_CENTER", "ALIGN_RIGHT" -> bodyPaint
                else -> bodyPaint
            }

            val alignment = when (block.type) {
                "ALIGN_CENTER" -> Layout.Alignment.ALIGN_CENTER
                "ALIGN_RIGHT" -> Layout.Alignment.ALIGN_OPPOSITE
                else -> Layout.Alignment.ALIGN_NORMAL
            }

            // Prep text (prepend bullet symbol if necessary)
            val displayText = if (block.type == "BULLET_LIST") {
                "•  ${block.text}"
            } else {
                block.text
            }

            if (displayText.isEmpty()) {
                yPosition += 15f // margin for blank lines
                continue
            }

            val indent = if (block.type == "BULLET_LIST") 15f else 0f

            val blockLayout = StaticLayout.Builder.obtain(displayText, 0, displayText.length, paintToUse, (printableWidth - indent).toInt())
                .setAlignment(alignment)
                .setLineSpacing(0f, 1.25f)
                .build()

            // Check page boundaries (bottom margin = MARGIN)
            if (yPosition + blockLayout.height > A4_HEIGHT - MARGIN) {
                // Draw footer first
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(currentPage)
                
                // Create next page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(A4_WIDTH, A4_HEIGHT, pageNumber).create()
                currentPage = pdfDocument.startPage(pageInfo)
                canvas = currentPage.canvas
                yPosition = MARGIN + 20f // start fresh at top margin
            }

            // Render block Layout
            canvas.save()
            canvas.translate(MARGIN + indent, yPosition)
            blockLayout.draw(canvas)
            canvas.restore()

            yPosition += blockLayout.height + 12f // paragraph spacing
        }

        // Draw footer on last page
        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(currentPage)

        // Write to file
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            // Sanitize filename
            val safeTitle = document.title.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val pdfFile = File(exportDir, "$safeTitle.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            pdfFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun drawFooter(canvas: Canvas, pageNumber: Int) {
        val footerText = "Page $pageNumber  |  A4 Write"
        val paint = Paint().apply {
            color = Color.rgb(120, 120, 120)
            textSize = 9f
            isAntiAlias = true
        }
        val textWidth = paint.measureText(footerText)
        canvas.drawText(footerText, (A4_WIDTH - textWidth) / 2f, A4_HEIGHT - 30f, paint)
    }
}
