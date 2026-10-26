package com.steven.smartrehab.utils

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.steven.smartrehab.data.local.SessionEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportGenerator {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 40f
    private const val RIGHT = PAGE_WIDTH - 40f
    private const val BOTTOM_LIMIT = PAGE_HEIGHT - 55f
    private const val ROW_H = 18f

    private val accent = Color.rgb(0, 121, 107)
    private val grey = Color.rgb(90, 96, 102)
    private val lightGrey = Color.rgb(215, 220, 222)

    private fun textPaint(size: Float, bold: Boolean = false, color: Int = Color.BLACK) = Paint().apply {
        isAntiAlias = true
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private val titlePaint = textPaint(22f, true, accent)
    private val subtitlePaint = textPaint(11f, false, grey)
    private val headingPaint = textPaint(13f, true, accent)
    private val bodyPaint = textPaint(11f)
    private val rowPaint = textPaint(9.5f)
    private val headerRowPaint = textPaint(9.5f, true)
    private val smallPaint = textPaint(9f, false, grey)
    private val footerPaint = textPaint(8.5f, false, grey)

    private val gridPaint = Paint().apply {
        color = lightGrey
        strokeWidth = 1f
        style = Paint.Style.STROKE
    }
    private val accentLinePaint = Paint().apply {
        isAntiAlias = true
        color = accent
        strokeWidth = 2f
        style = Paint.Style.STROKE
    }
    private val accentFillPaint = Paint().apply {
        isAntiAlias = true
        color = accent
        style = Paint.Style.FILL
    }
    private val headerBgPaint = Paint().apply {
        color = Color.rgb(228, 240, 238)
        style = Paint.Style.FILL
    }

    /** Gere les pages et la position verticale courante. */
    private class Writer(private val document: PdfDocument) {
        private var pageNumber = 0
        private var current: PdfDocument.Page? = null
        lateinit var canvas: Canvas
        var y = MARGIN

        fun newPage() {
            current?.let { document.finishPage(it) }
            pageNumber++
            val info = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            val page = document.startPage(info)
            current = page
            canvas = page.canvas
            y = MARGIN
            canvas.drawText(
                "Outil de suivi personnel, ne constitue pas un dispositif médical.",
                MARGIN, PAGE_HEIGHT - 34f, footerPaint
            )
            canvas.drawText("SmartRehab AI, page $pageNumber", MARGIN, PAGE_HEIGHT - 22f, footerPaint)
        }

        fun ensure(height: Float) {
            if (y + height > BOTTOM_LIMIT) newPage()
        }

        fun finish() {
            current?.let { document.finishPage(it) }
            current = null
        }
    }

    fun generate(
        context: Context,
        patientName: String,
        sessions: List<SessionEntity>,
        filterLabel: String
    ): File {
        val dir = File(context.cacheDir, "reports").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() }

        val stamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.FRANCE).format(Date())
        val file = File(dir, "rapport_smartrehab_$stamp.pdf")

        val ordered = sessions.sortedBy { it.date }
        val document = PdfDocument()
        val writer = Writer(document)

        try {
            writer.newPage()
            drawHeader(writer, patientName, ordered, filterLabel)
            drawSummary(writer, ordered)
            drawExerciseTable(writer, ordered)
            drawChart(writer, ordered)
            drawSessionTable(writer, ordered)
            drawNotes(writer)
            writer.finish()
            FileOutputStream(file).use { document.writeTo(it) }
        } finally {
            writer.finish()
            document.close()
        }
        return file
    }

    fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Rapport SmartRehab AI")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Partager le rapport")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    // ---------- Sections ----------

    private fun drawHeader(
        w: Writer,
        patientName: String,
        sessions: List<SessionEntity>,
        filterLabel: String
    ) {
        val dateFmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)

        w.canvas.drawText("Rapport de rééducation", MARGIN, w.y + 20f, titlePaint)
        w.y += 32f
        w.canvas.drawText("SmartRehab AI", MARGIN, w.y + 4f, subtitlePaint)
        w.y += 12f
        w.canvas.drawLine(MARGIN, w.y, RIGHT, w.y, accentLinePaint)
        w.y += 22f

        line(w, "Patient : ${patientName.ifBlank { "Non renseigné" }}")
        line(w, "Rapport généré le : ${dateFmt.format(Date())}")
        line(w, "Exercices inclus : $filterLabel")
        if (sessions.isNotEmpty()) {
            val from = dateFmt.format(Date(sessions.first().date))
            val to = dateFmt.format(Date(sessions.last().date))
            line(w, "Période : du $from au $to")
        }
        w.y += 6f
    }

    private fun drawSummary(w: Writer, sessions: List<SessionEntity>) {
        heading(w, "Synthèse")
        if (sessions.isEmpty()) {
            line(w, "Aucune séance enregistrée.")
            return
        }

        val totalReps = sessions.sumOf { it.totalReps }
        val correct = sessions.sumOf { it.correctReps }
        val incorrect = sessions.sumOf { it.incorrectReps }

        line(w, "Nombre de séances : ${sessions.size}")
        line(w, "Répétitions : $totalReps (correctes : $correct, à corriger : $incorrect)")
        line(w, "Score moyen : ${scoreOf(sessions)} %")
        line(w, "Durée totale : ${formatDuration(sessions.sumOf { it.durationSeconds })}")

        if (sessions.size >= 4) {
            val half = sessions.size / 2
            val diff = scoreOf(sessions.takeLast(half)) - scoreOf(sessions.take(half))
            val sign = if (diff > 0) "+" else ""
            line(w, "Évolution du score entre la première et la seconde moitié des séances : $sign$diff points")
        }
    }

    private fun drawExerciseTable(w: Writer, sessions: List<SessionEntity>) {
        if (sessions.isEmpty()) return
        w.ensure(100f)
        heading(w, "Détail par exercice")

        val xs = listOf(MARGIN, 300f, 380f, 480f)
        tableRow(w, listOf("Exercice", "Séances", "Répétitions", "Score"), xs, headerRowPaint, true)

        sessions.groupBy { it.exerciseName }.forEach { (name, list) ->
            tableRow(
                w,
                listOf(name, "${list.size}", "${list.sumOf { it.totalReps }}", "${scoreOf(list)} %"),
                xs,
                rowPaint
            )
        }
        w.y += 6f
    }

    private fun drawChart(w: Writer, sessions: List<SessionEntity>) {
        if (sessions.size < 2) return

        val chartHeight = 120f
        w.ensure(chartHeight + 70f)
        heading(w, "Évolution du score par séance (%)")

        val data = sessions.takeLast(20)
        val scores = data.map {
            if (it.totalReps > 0) it.correctReps * 100f / it.totalReps else 0f
        }

        val left = MARGIN + 28f
        val top = w.y + 4f
        val bottom = top + chartHeight
        val width = RIGHT - left

        listOf(0, 50, 100).forEach { value ->
            val gy = bottom - value / 100f * chartHeight
            w.canvas.drawLine(left, gy, RIGHT, gy, gridPaint)
            w.canvas.drawText("$value", MARGIN, gy + 3f, smallPaint)
        }

        val stepX = width / (scores.size - 1)
        val points = scores.mapIndexed { i, s ->
            Pair(left + i * stepX, bottom - s / 100f * chartHeight)
        }

        for (i in 0 until points.size - 1) {
            w.canvas.drawLine(
                points[i].first, points[i].second,
                points[i + 1].first, points[i + 1].second,
                accentLinePaint
            )
        }
        points.forEach { (px, py) -> w.canvas.drawCircle(px, py, 3f, accentFillPaint) }

        w.canvas.drawText(
            "Séances les plus anciennes à gauche, les plus récentes à droite (20 dernières au maximum).",
            left, bottom + 14f, smallPaint
        )
        w.y = bottom + 26f
    }

    private fun drawSessionTable(w: Writer, sessions: List<SessionEntity>) {
        if (sessions.isEmpty()) return
        w.ensure(100f)
        heading(w, "Détail des séances")

        val xs = listOf(MARGIN, 118f, 330f, 372f, 414f, 462f, 514f)
        val headers = listOf("Date", "Exercice", "Rép.", "Corr.", "Score", "Angle", "Durée")
        tableRow(w, headers, xs, headerRowPaint, true)

        val fmt = SimpleDateFormat("dd/MM/yy HH:mm", Locale.FRANCE)
        sessions.forEach { s ->
            if (w.y + ROW_H > BOTTOM_LIMIT) {
                w.newPage()
                tableRow(w, headers, xs, headerRowPaint, true)
            }
            val score = if (s.totalReps > 0) s.correctReps * 100 / s.totalReps else 0
            val angle = if (s.averageAmplitude > 0.0) "${s.averageAmplitude.toInt()}°" else "--"
            tableRow(
                w,
                listOf(
                    fmt.format(Date(s.date)),
                    s.exerciseName,
                    "${s.totalReps}",
                    "${s.correctReps}",
                    "$score %",
                    angle,
                    formatShortDuration(s.durationSeconds)
                ),
                xs,
                rowPaint
            )
        }
        w.y += 8f
    }

    private fun drawNotes(w: Writer) {
        w.ensure(70f)
        w.y += 6f
        val notes = listOf(
            "Rép. : répétitions. Corr. : répétitions correctes. Angle : angle articulaire moyen atteint",
            "au point extrême de chaque répétition.",
            "Les seuils d'amplitude utilisés pour juger une répétition sont indicatifs. Ce rapport est généré",
            "par une analyse vidéo en deux dimensions et ne remplace pas l'évaluation d'un professionnel de santé."
        )
        notes.forEach { text ->
            w.ensure(13f)
            w.canvas.drawText(text, MARGIN, w.y + 9f, smallPaint)
            w.y += 12f
        }
    }

    // ---------- Utilitaires ----------

    private fun heading(w: Writer, text: String) {
        w.ensure(40f)
        w.y += 8f
        w.canvas.drawText(text, MARGIN, w.y + 13f, headingPaint)
        w.y += 24f
    }

    private fun line(w: Writer, text: String) {
        w.ensure(17f)
        w.canvas.drawText(text, MARGIN, w.y + 11f, bodyPaint)
        w.y += 17f
    }

    private fun tableRow(
        w: Writer,
        cells: List<String>,
        xs: List<Float>,
        paint: Paint,
        header: Boolean = false
    ) {
        w.ensure(ROW_H)
        if (header) {
            w.canvas.drawRect(MARGIN, w.y, RIGHT, w.y + ROW_H, headerBgPaint)
        }
        cells.forEachIndexed { i, text ->
            w.canvas.drawText(text, xs[i] + 3f, w.y + 12.5f, paint)
        }
        w.canvas.drawLine(MARGIN, w.y + ROW_H, RIGHT, w.y + ROW_H, gridPaint)
        w.y += ROW_H
    }

    private fun scoreOf(list: List<SessionEntity>): Int {
        val total = list.sumOf { it.totalReps }
        return if (total > 0) list.sumOf { it.correctReps } * 100 / total else 0
    }

    private fun formatDuration(totalSeconds: Int): String =
        "${totalSeconds / 60} min ${(totalSeconds % 60).toString().padStart(2, '0')} s"

    private fun formatShortDuration(totalSeconds: Int): String =
        "${totalSeconds / 60}:${(totalSeconds % 60).toString().padStart(2, '0')}"
}