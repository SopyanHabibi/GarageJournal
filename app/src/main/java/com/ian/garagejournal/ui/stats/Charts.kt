package com.ian.garagejournal.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ian.garagejournal.domain.EfficiencyPoint
import com.ian.garagejournal.domain.MonthlyCost
import com.ian.garagejournal.util.formatDate
import com.ian.garagejournal.util.formatRupiah
import com.ian.garagejournal.util.formatRupiahCompact
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ChartHeight = 160.dp
private val monthShort = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("id-ID"))
private val monthLong = DateTimeFormatter.ofPattern("MMM yyyy", Locale.forLanguageTag("id-ID"))

fun monthLabel(month: YearMonth): String = month.format(monthShort)
fun monthTitle(month: YearMonth): String = month.format(monthLong)

@Composable
fun EfficiencyLineChart(
    points: List<EfficiencyPoint>,
    average: Double?,
    modifier: Modifier = Modifier
) {
    if (points.isEmpty()) return

    val lineColor = MaterialTheme.colorScheme.primary
    val averageColor = MaterialTheme.colorScheme.tertiary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val values = points.map { it.kmPerLiter }
    val scale = values + listOfNotNull(average)
    val minV = scale.min()
    val maxV = scale.max()
    val span = (maxV - minV).takeIf { it > 0.0 } ?: 1.0
    val low = minV - span * 0.1 // sedikit ruang supaya titik tidak menempel ke tepi
    val high = maxV + span * 0.1

    val firstDate = points.first().date
    val dateSpan = points.last().date - firstDate

    val description = "Grafik tren KM per liter dengan ${points.size} titik. " +
            "Terakhir ${"%.1f".format(values.last())}, " +
            "rentang ${"%.1f".format(minV)} sampai ${"%.1f".format(maxV)}."

    Column(
        modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Skala: ${"%.1f".format(minV)} - ${"%.1f".format(maxV)} km/L",
            style = MaterialTheme.typography.labelSmall
        )

        Canvas(Modifier.fillMaxWidth().height(ChartHeight)) {
            val w = size.width
            val h = size.height
            val pad = 8.dp.toPx()

            for (i in 0..2) {
                val y = h * i / 2f
                drawLine(gridColor, Offset(0f, y), Offset(w, y), strokeWidth = 1.dp.toPx())
            }

            // Jarak mendatar mengikuti tanggal sebenarnya, bukan urutan
            fun xAt(index: Int, date: Long): Float = when {
                points.size == 1 -> w / 2f
                dateSpan > 0 -> pad + (w - 2 * pad) * ((date - firstDate).toDouble() / dateSpan).toFloat()
                else -> pad + (w - 2 * pad) * index / (points.size - 1)
            }

            fun yAt(v: Double): Float = (h * (1 - (v - low) / (high - low))).toFloat()

            average?.let {
                val y = yAt(it)
                drawLine(
                    averageColor, Offset(0f, y), Offset(w, y),
                    strokeWidth = 1.5f.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f))
                )
            }

            val path = Path()
            points.forEachIndexed { i, p ->
                val x = xAt(i, p.date)
                val y = yAt(p.kmPerLiter)
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path, lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            points.forEachIndexed { i, p ->
                drawCircle(lineColor, radius = 4.dp.toPx(), center = Offset(xAt(i, p.date), yAt(p.kmPerLiter)))
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDate(points.first().date), style = MaterialTheme.typography.labelSmall)
            Text(formatDate(points.last().date), style = MaterialTheme.typography.labelSmall)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(lineColor, "KM/L tiap isi penuh")
            if (average != null) LegendItem(averageColor, "Rata-rata")
        }
    }
}

@Composable
fun MonthlyCostBarChart(data: List<MonthlyCost>, modifier: Modifier = Modifier) {
    val serviceColor = MaterialTheme.colorScheme.tertiary
    val fuelColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val maxTotal = data.maxOfOrNull { it.total } ?: 0L
    if (maxTotal == 0L) {
        Text(
            "Belum ada biaya tercatat pada periode ini.",
            modifier,
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }

    val description = "Grafik biaya per bulan untuk ${data.size} bulan. " +
            "Bulan tertinggi ${formatRupiah(maxTotal)}."

    Column(
        modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            "Batang tertinggi: ${formatRupiahCompact(maxTotal)}",
            style = MaterialTheme.typography.labelSmall
        )

        Canvas(Modifier.fillMaxWidth().height(ChartHeight)) {
            val slot = size.width / data.size
            val barWidth = slot * 0.6f
            val h = size.height

            drawLine(gridColor, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx())
            drawLine(gridColor, Offset(0f, h), Offset(size.width, h), strokeWidth = 1.dp.toPx())

            data.forEachIndexed { i, m ->
                val left = slot * i + (slot - barWidth) / 2f
                val serviceH = h * (m.serviceCost.toFloat() / maxTotal)
                val fuelH = h * (m.fuelCost.toFloat() / maxTotal)
                // servis di bawah, BBM menumpuk di atasnya
                drawRect(serviceColor, Offset(left, h - serviceH), Size(barWidth, serviceH))
                drawRect(fuelColor, Offset(left, h - serviceH - fuelH), Size(barWidth, fuelH))
            }
        }

        Row(Modifier.fillMaxWidth()) {
            data.forEach {
                Text(
                    monthLabel(it.month),
                    Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(serviceColor, "Servis")
            LegendItem(fuelColor, "BBM")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color))
        Spacer(Modifier.width(6.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}