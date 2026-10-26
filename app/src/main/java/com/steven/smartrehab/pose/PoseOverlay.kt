package com.steven.smartrehab.pose

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import kotlin.math.max

private val connections = listOf(
    11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16,
    11 to 23, 12 to 24, 23 to 24,
    23 to 25, 25 to 27, 24 to 26, 26 to 28
)

@Composable
fun PoseOverlay(
    landmarks: List<NormalizedLandmark>,
    imageWidth: Int,
    imageHeight: Int,
    highlight: List<Int>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        if (landmarks.size < 29 || imageWidth == 0 || imageHeight == 0) return@Canvas

        // Même logique que PreviewView (FILL_CENTER) + miroir de la caméra frontale
        val scale = max(size.width / imageWidth, size.height / imageHeight)
        val dx = (size.width - imageWidth * scale) / 2f
        val dy = (size.height - imageHeight * scale) / 2f

        fun point(i: Int): Offset {
            val lm = landmarks[i]
            return Offset(
                (1f - lm.x()) * imageWidth * scale + dx,
                lm.y() * imageHeight * scale + dy
            )
        }

        connections.forEach { (a, b) ->
            drawLine(
                color = Color.White,
                start = point(a),
                end = point(b),
                strokeWidth = 5f,
                cap = StrokeCap.Round
            )
        }

        for (i in 11..28) {
            drawCircle(
                color = if (i in highlight) Color(0xFFFFC107) else Color(0xFF4CAF50),
                radius = if (i in highlight) 14f else 8f,
                center = point(i)
            )
        }
    }
}