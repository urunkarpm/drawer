package com.urunkarpm.drawer.feature.home.component

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper
import android.util.Size
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat

import android.os.HandlerThread
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Native standard-library Camera2 front camera preview.
 * Configures center-crop matrix transform to preserve 1:1 natural aspect ratio without distortion or elongation.
 *
 * ponytail: matrix-based center-crop scales TextureView natively on GPU without stretching; ceiling is fixed front camera facing; upgrade path is multi-camera switcher.
 */
@SuppressLint("MissingPermission")
@Composable
fun FrontCameraPreview(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val cameraManager = remember(context) {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }

    val (frontCameraId, optimalSize) = remember(cameraManager) {
        try {
            val id = cameraManager.cameraIdList.firstOrNull { camId ->
                val chars = cameraManager.getCameraCharacteristics(camId)
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: cameraManager.cameraIdList.firstOrNull()

            val size = if (id != null) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val map = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val outputSizes = map?.getOutputSizes(SurfaceTexture::class.java)
                // Prefer 1080p or 720p 4:3 / 16:9 for clean, sharp, natural preview
                outputSizes?.filter { it.width <= 1920 && it.height <= 1440 && it.width >= 640 }
                    ?.maxByOrNull { it.width * it.height }
                    ?: outputSizes?.firstOrNull()
                    ?: Size(1280, 720)
            } else {
                Size(1280, 720)
            }
            id to size
        } catch (_: Exception) {
            null to Size(1280, 720)
        }
    }

    class CameraHolder {
        var cameraDevice: CameraDevice? = null
        var captureSession: CameraCaptureSession? = null
        fun close() {
            try {
                captureSession?.close()
                captureSession = null
                cameraDevice?.close()
                cameraDevice = null
            } catch (_: Exception) {}
        }
    }

    val holder = remember { CameraHolder() }

    val cameraThread = remember {
        HandlerThread("Camera2Background").apply { start() }
    }
    val cameraHandler = remember(cameraThread) {
        Handler(cameraThread.looper)
    }

    DisposableEffect(Unit) {
        onDispose {
            holder.close()
            cameraThread.quitSafely()
        }
    }

    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) {
        holder.close()
    }

    AndroidView(
        factory = { ctx ->
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
                        applyCenterCropTransform(this@apply, width, height, optimalSize)
                        surfaceTexture.setDefaultBufferSize(optimalSize.width, optimalSize.height)

                        if (frontCameraId == null) return
                        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return

                        try {
                            cameraManager.openCamera(frontCameraId, object : CameraDevice.StateCallback() {
                                override fun onOpened(camera: CameraDevice) {
                                    holder.cameraDevice = camera
                                    val surface = Surface(surfaceTexture)
                                    try {
                                        val builder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                                            addTarget(surface)
                                        }
                                        camera.createCaptureSession(
                                            listOf(surface),
                                            object : CameraCaptureSession.StateCallback() {
                                                override fun onConfigured(session: CameraCaptureSession) {
                                                    holder.captureSession = session
                                                    try {
                                                        session.setRepeatingRequest(builder.build(), null, cameraHandler)
                                                    } catch (_: Exception) {}
                                                }

                                                override fun onConfigureFailed(session: CameraCaptureSession) {}
                                            },
                                            cameraHandler
                                        )
                                    } catch (_: Exception) {
                                        camera.close()
                                    }
                                }

                                override fun onDisconnected(camera: CameraDevice) {
                                    holder.close()
                                }

                                override fun onError(camera: CameraDevice, error: Int) {
                                    holder.close()
                                }
                            }, cameraHandler)
                        } catch (_: Exception) {}
                    }


                    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
                        applyCenterCropTransform(this@apply, width, height, optimalSize)
                    }

                    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                        holder.close()
                        return true
                    }

                    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                }
            }
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * Applies center-crop transformation to prevent image stretching/elongation and mirrors horizontally.
 */
private fun applyCenterCropTransform(textureView: TextureView, viewWidth: Int, viewHeight: Int, previewSize: Size) {
    if (viewWidth <= 0 || viewHeight <= 0) return

    val matrix = Matrix()
    val viewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
    val centerX = viewRect.centerX()
    val centerY = viewRect.centerY()

    // Sensor output is in landscape coordinate space (e.g. 1280x720).
    // In portrait, the camera frame is previewSize.height (width) : previewSize.width (height).
    val previewAspect = previewSize.height.toFloat() / previewSize.width.toFloat()
    val viewAspect = viewWidth.toFloat() / viewHeight.toFloat()

    val scaleX: Float
    val scaleY: Float
    if (viewAspect > previewAspect) {
        // View is wider than camera stream aspect ratio: fit width and crop top/bottom
        scaleX = 1f
        scaleY = (viewWidth.toFloat() / previewAspect) / viewHeight.toFloat()
    } else {
        // View is narrower than camera stream aspect ratio: fit height and crop sides
        scaleX = (viewHeight.toFloat() * previewAspect) / viewWidth.toFloat()
        scaleY = 1f
    }

    // scaleX without negation shows the camera feed as is (unmirrored)
    matrix.setScale(scaleX, scaleY, centerX, centerY)
    textureView.setTransform(matrix)
}
