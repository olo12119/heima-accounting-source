package com.heima.accounting.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.heima.accounting.R
import kotlin.math.roundToInt

private data class Sprite(val column: Int, val row: Int)

private val sprites = mapOf(
    "meal" to Sprite(0, 0),
    "transport" to Sprite(1, 0),
    "shopping" to Sprite(2, 0),
    "home" to Sprite(3, 0),
    "entertainment" to Sprite(4, 0),
    "health" to Sprite(5, 0),
    "education" to Sprite(6, 0),
    "gift" to Sprite(1, 1),
    "travel" to Sprite(2, 1),
    "other" to Sprite(3, 1),
    "part_time" to Sprite(4, 1),
    "business" to Sprite(6, 1),
    "investment" to Sprite(0, 2),
    "salary" to Sprite(2, 2),
    "refund" to Sprite(3, 2),
    "pet" to Sprite(0, 3),
)

private val LocalCategoryAtlas = staticCompositionLocalOf<ImageBitmap?> { null }

/**
 * Decodes the 3D category atlas once for the whole app instead of once per icon.
 * This prevents a multi-megabyte bitmap decode burst whenever the record sheet opens.
 */
@Composable
fun ProvideCategoryArtwork(content: @Composable () -> Unit) {
    val resources = LocalResources.current
    val atlas = remember(resources) {
        ImageBitmap.imageResource(resources, R.drawable.category_3d_atlas)
    }
    CompositionLocalProvider(LocalCategoryAtlas provides atlas, content = content)
}

@Composable
fun CategoryArtwork(
    iconKey: String,
    modifier: Modifier = Modifier,
) {
    val atlas = LocalCategoryAtlas.current ?: ImageBitmap.imageResource(R.drawable.category_3d_atlas)
    val sprite = remember(iconKey) { sprites[iconKey] ?: sprites.getValue("other") }
    Canvas(modifier) {
        // 原始图集是7列×4行。保留少量透明边缘，避免阴影被切掉。
        val cellWidth = atlas.width / 7
        val cellHeight = atlas.height / 4
        val insetX = (cellWidth * 0.025f).roundToInt()
        val insetY = (cellHeight * 0.015f).roundToInt()
        val sourceOffset = IntOffset(sprite.column * cellWidth + insetX, sprite.row * cellHeight + insetY)
        val sourceSize = IntSize(cellWidth - insetX * 2, cellHeight - insetY * 2)
        drawIntoCanvas {
            drawImage(
                image = atlas,
                srcOffset = sourceOffset,
                srcSize = sourceSize,
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}
