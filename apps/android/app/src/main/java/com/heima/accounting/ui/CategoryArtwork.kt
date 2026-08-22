package com.heima.accounting.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.heima.accounting.R
import com.heima.accounting.designsystem.HeimaTheme

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

/** Decodes the 3D atlas once for the whole app instead of once per icon. */
@Composable
fun ProvideCategoryArtwork(content: @Composable () -> Unit) {
    val resources = LocalResources.current
    val atlas = remember(resources) {
        ImageBitmap.imageResource(resources, R.drawable.category_3d_atlas_v2)
    }
    CompositionLocalProvider(LocalCategoryAtlas provides atlas, content = content)
}

@Composable
fun CategoryArtwork(
    iconKey: String,
    modifier: Modifier = Modifier,
) {
    val atlas = LocalCategoryAtlas.current ?: ImageBitmap.imageResource(R.drawable.category_3d_atlas_v2)
    val sprite = remember(iconKey) { sprites[iconKey] ?: sprites.getValue("other") }
    Canvas(modifier) {
        // The generated v2 atlas owns the safe area and optical centering. Drawing a
        // complete cell avoids one-off screen adjustments and neighbouring-cell bleed.
        val cellWidth = atlas.width / 7
        val cellHeight = atlas.height / 4
        drawIntoCanvas {
            drawImage(
                image = atlas,
                srcOffset = IntOffset(sprite.column * cellWidth, sprite.row * cellHeight),
                srcSize = IntSize(cellWidth, cellHeight),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}

/** The shared production container used by every category illustration. */
@Composable
fun CategoryIcon(
    iconKey: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
) {
    val palette = HeimaTheme.palette
    val motion = HeimaTheme.motion
    val baseColor by animateColorAsState(
        targetValue = if (selected) palette.brandSoft else palette.surface,
        label = "category_icon_surface",
    )
    val strokeColor by animateColorAsState(
        targetValue = if (selected) {
            palette.brand.copy(alpha = .66f)
        } else {
            palette.glassStroke.copy(alpha = if (motion.darkTheme) .34f else .82f)
        },
        label = "category_icon_stroke",
    )

    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = if (selected) 10.dp else 4.dp,
                shape = CircleShape,
                ambientColor = palette.brand.copy(alpha = .14f),
                spotColor = Color.Black.copy(alpha = .16f),
            )
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        baseColor.copy(alpha = if (motion.darkTheme) .92f else .98f),
                        palette.surfaceMuted.copy(alpha = if (motion.darkTheme) .88f else .94f),
                    ),
                ),
            )
            .border(1.dp, strokeColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        CategoryArtwork(iconKey, Modifier.fillMaxSize())
    }
}
