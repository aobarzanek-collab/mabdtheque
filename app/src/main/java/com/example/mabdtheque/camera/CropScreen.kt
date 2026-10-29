package com.example.mabdtheque.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

private enum class Corner { TopLeft, TopRight, BottomLeft, BottomRight }

private fun rotateBitmap(source: Bitmap, degrees: Float): Bitmap {
    val matrix = android.graphics.Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun moveCorner(
    rect: Rect, corner: Corner, drag: Offset, bounds: Rect, minSize: Float
): Rect {
    var l = rect.left
    var t = rect.top
    var r = rect.right
    var b = rect.bottom
    when (corner) {
        Corner.TopLeft -> { l += drag.x; t += drag.y }
        Corner.TopRight -> { r += drag.x; t += drag.y }
        Corner.BottomLeft -> { l += drag.x; b += drag.y }
        Corner.BottomRight -> { r += drag.x; b += drag.y }
    }
    l = l.coerceIn(bounds.left, r - minSize)
    t = t.coerceIn(bounds.top, b - minSize)
    r = r.coerceIn(l + minSize, bounds.right)
    b = b.coerceIn(t + minSize, bounds.bottom)
    return Rect(l, t, r, b)
}

private fun moveRect(rect: Rect, drag: Offset, bounds: Rect): Rect {
    val w = rect.width
    val h = rect.height
    val left = (rect.left + drag.x).coerceIn(bounds.left, bounds.right - w)
    val top = (rect.top + drag.y).coerceIn(bounds.top, bounds.bottom - h)
    return Rect(left, top, left + w, top + h)
}

@Composable
fun CropScreen(
    path: String,
    onDone: (String) -> Unit,
    onCancel: () -> Unit
) {
    val initialBitmap = remember(path) { BitmapFactory.decodeFile(path) }
    if (initialBitmap == null) {
        onCancel()
        return
    }
    var bitmap by remember(path) { mutableStateOf(initialBitmap) }
    var rotation by remember(path) { mutableStateOf(0f) }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color.Black)
    ) {
        val density = LocalDensity.current
        val boxW = constraints.maxWidth.toFloat()
        val boxH = constraints.maxHeight.toFloat()
        val scale = min(boxW / bitmap.width, (boxH * 0.7f) / bitmap.height)
        val imgW = bitmap.width * scale
        val imgH = bitmap.height * scale
        val imgLeft = (boxW - imgW) / 2f
        val imgTop = boxH * 0.05f
        val imageRect = Rect(imgLeft, imgTop, imgLeft + imgW, imgTop + imgH)
        val minCrop = with(density) { 40.dp.toPx() }

        var crop by remember(path, bitmap) {
            mutableStateOf(
                Rect(
                    imageRect.left + imgW * 0.06f,
                    imageRect.top + imgH * 0.06f,
                    imageRect.right - imgW * 0.06f,
                    imageRect.bottom - imgH * 0.06f
                )
            )
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawImage(
                image = bitmap.asImageBitmap(),
                dstOffset = IntOffset(imgLeft.roundToInt(), imgTop.roundToInt()),
                dstSize = Size(imgW, imgH)
            )
            val dim = Color.Black.copy(alpha = 0.55f)
            drawRect(dim, topLeft = Offset(imageRect.left, imageRect.top),
                size = Size(imgW, crop.top - imageRect.top))
            drawRect(dim, topLeft = Offset(imageRect.left, crop.bottom),
                size = Size(imgW, imageRect.bottom - crop.bottom))
            drawRect(dim, topLeft = Offset(imageRect.left, crop.top),
                size = Size(crop.left - imageRect.left, crop.height))
            drawRect(dim, topLeft = Offset(crop.right, crop.top),
                size = Size(imageRect.right - crop.right, crop.height))
            drawRect(
                color = Color.White,
                topLeft = Offset(crop.left, crop.top),
                size = Size(crop.width, crop.height),
                style = Stroke(width = 3f)
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(crop.left.roundToInt(), crop.top.roundToInt()) }
                .size(
                    with(density) { crop.width.toDp() },
                    with(density) { crop.height.toDp() }
                )
                .pointerInput(path, bitmap) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        crop = moveRect(crop, drag, imageRect)
                    }
                }
        )

        val handlePx = with(density) { 28.dp.toPx() }
        Corner.entries.forEach { corner ->
            val c = corner
            Box(
                modifier = Modifier
                    .offset {
                        val (x, y) = when (c) {
                            Corner.TopLeft -> crop.left to crop.top
                            Corner.TopRight -> crop.right to crop.top
                            Corner.BottomLeft -> crop.left to crop.bottom
                            Corner.BottomRight -> crop.right to crop.bottom
                        }
                        IntOffset(
                            (x - handlePx / 2).roundToInt(),
                            (y - handlePx / 2).roundToInt()
                        )
                    }
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .pointerInput(c, path, bitmap) {
                        detectDragGestures { change, drag ->
                            change.consume()
                            crop = moveCorner(crop, c, drag, imageRect, minCrop)
                        }
                    }
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = {
                    bitmap = rotateBitmap(bitmap, -90f)
                    rotation -= 90f
                }) { Text("⟲ Pivoter gauche") }
                OutlinedButton(onClick = {
                    bitmap = rotateBitmap(bitmap, 90f)
                    rotation += 90f
                }) { Text("Pivoter droite ⟳") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = onCancel) { Text("Annuler") }
                Button(
                    onClick = {
                        val l = ((crop.left - imgLeft) / scale).roundToInt()
                            .coerceIn(0, bitmap.width - 1)
                        val t = ((crop.top - imgTop) / scale).roundToInt()
                            .coerceIn(0, bitmap.height - 1)
                        val r = ((crop.right - imgLeft) / scale).roundToInt()
                            .coerceIn(l + 1, bitmap.width)
                        val b = ((crop.bottom - imgTop) / scale).roundToInt()
                            .coerceIn(t + 1, bitmap.height)
                        val cropped = Bitmap.createBitmap(bitmap, l, t, r - l, b - t)
                        File(path).outputStream().use { out ->
                            cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
                        }
                        onDone(path)
                    }
                ) { Text("Valider le recadrage") }
            }
        }
    }
}
