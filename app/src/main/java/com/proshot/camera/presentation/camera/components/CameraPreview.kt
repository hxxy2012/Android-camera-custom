package com.proshot.camera.presentation.camera.components

import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.proshot.camera.presentation.camera.CameraViewModel
import com.proshot.camera.presentation.camera.GridType
import com.proshot.camera.presentation.theme.AccentOrange
import timber.log.Timber

/**
 * Camera preview with grid overlay
 */
@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    viewModel: CameraViewModel,
    gridType: GridType = GridType.NONE
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val textureView = remember {
        TextureView(context).apply {
            keepScreenOn = true
        }
    }

    // Lifecycle observer to manage camera
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    Timber.d("Lifecycle: ON_RESUME - Starting camera")
                }
                Lifecycle.Event.ON_PAUSE -> {
                    Timber.d("Lifecycle: ON_PAUSE")
                }
                else -> {}
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Start camera when texture is available
    LaunchedEffect(textureView) {
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Timber.d("Surface texture available: ${width}x${height}")

                // Open camera with the main back camera
                val previewSurface = Surface(surface)
                viewModel.openCamera("0", previewSurface) // "0" is usually the back camera
            }

            override fun onSurfaceTextureSizeChanged(
                surface: android.graphics.SurfaceTexture,
                width: Int,
                height: Int
            ) {
                Timber.d("Surface texture size changed: ${width}x${height}")
            }

            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean {
                Timber.d("Surface texture destroyed")
                return true
            }

            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
                // Called every frame - don't log to avoid spam
            }
        }
    }

    Box(modifier = modifier) {
        // Camera preview
        AndroidView(
            factory = { textureView },
            modifier = Modifier.fillMaxSize()
        )

        // Grid overlay
        if (gridType != GridType.NONE) {
            GridOverlay(
                modifier = Modifier.fillMaxSize(),
                gridType = gridType
            )
        }
    }
}

/**
 * Grid overlay for composition assistance
 */
@Composable
fun GridOverlay(
    modifier: Modifier = Modifier,
    gridType: GridType,
    color: Color = AccentOrange.copy(alpha = 0.5f)
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val stroke = Stroke(
            width = 2f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )

        when (gridType) {
            GridType.RULE_OF_THIRDS -> {
                // Vertical lines
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(width / 3, 0f),
                    end = androidx.compose.ui.geometry.Offset(width / 3, height),
                    strokeWidth = 2f
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(2 * width / 3, 0f),
                    end = androidx.compose.ui.geometry.Offset(2 * width / 3, height),
                    strokeWidth = 2f
                )

                // Horizontal lines
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, height / 3),
                    end = androidx.compose.ui.geometry.Offset(width, height / 3),
                    strokeWidth = 2f
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, 2 * height / 3),
                    end = androidx.compose.ui.geometry.Offset(width, 2 * height / 3),
                    strokeWidth = 2f
                )
            }

            GridType.GOLDEN_RATIO -> {
                val phi = 1.618f

                // Vertical lines (golden ratio)
                val x1 = width / phi
                val x2 = width - (width / phi)

                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(x1, 0f),
                    end = androidx.compose.ui.geometry.Offset(x1, height),
                    strokeWidth = 2f
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(x2, 0f),
                    end = androidx.compose.ui.geometry.Offset(x2, height),
                    strokeWidth = 2f
                )

                // Horizontal lines (golden ratio)
                val y1 = height / phi
                val y2 = height - (height / phi)

                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, y1),
                    end = androidx.compose.ui.geometry.Offset(width, y1),
                    strokeWidth = 2f
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, y2),
                    end = androidx.compose.ui.geometry.Offset(width, y2),
                    strokeWidth = 2f
                )
            }

            GridType.DIAGONAL -> {
                // Diagonal lines from corners
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                    end = androidx.compose.ui.geometry.Offset(width, height),
                    strokeWidth = 2f
                )
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(width, 0f),
                    end = androidx.compose.ui.geometry.Offset(0f, height),
                    strokeWidth = 2f
                )
            }

            GridType.NONE -> {
                // No grid
            }
        }
    }
}
