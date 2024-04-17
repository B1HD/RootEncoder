/*
 * Copyright (C) 2023 pedroSG94.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.pedro.library.util.sources.video

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.os.Build
import android.util.Range
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.WindowManager
import androidx.annotation.Nullable
import androidx.annotation.RequiresApi
import com.pedro.encoder.input.video.Camera2ApiManager
import com.pedro.encoder.input.video.CameraHelper
import com.pedro.encoder.input.video.facedetector.FaceDetectorCallback

/**
 * Created by pedro on 11/1/24.
 */
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
class Camera2Source(context: Context): VideoSource() {

  private val camera = Camera2ApiManager(context)
  private var facing = CameraHelper.Facing.BACK
  private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

  override fun create(width: Int, height: Int, fps: Int, rotation: Int): Boolean {
    val result = checkResolutionSupported(width, height)
    if (!result) {
      throw IllegalArgumentException("Unsupported resolution: ${width}x$height")
    }
    return true
  }

  override fun start(surfaceTexture: SurfaceTexture) {
    this.surfaceTexture = surfaceTexture
    if (!isRunning()) {
      surfaceTexture.setDefaultBufferSize(width, height)
      camera.prepareCamera(surfaceTexture, width, height, fps, facing)
      camera.openCameraFacing(facing)
    }
  }

  override fun stop() {
    if (isRunning()) camera.closeCamera()
  }

  override fun release() {}

  override fun isRunning(): Boolean = camera.isRunning

  private fun checkResolutionSupported(width: Int, height: Int): Boolean {
    if (width % 2 != 0 || height % 2 != 0) {
      throw IllegalArgumentException("width and height values must be divisible by 2")
    }
    val size = Size(width, height)
    val resolutions = if (facing == CameraHelper.Facing.BACK) {
      camera.cameraResolutionsBack
    } else camera.cameraResolutionsFront
    return if (camera.levelSupported == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      //this is a wrapper of camera1 api. Only listed resolutions are supported
      resolutions.contains(size)
    } else {
      val widthList = resolutions.map { size.width }
      val heightList = resolutions.map { size.height }
      val maxWidth = widthList.maxOrNull() ?: 0
      val maxHeight = heightList.maxOrNull() ?: 0
      val minWidth = widthList.minOrNull() ?: 0
      val minHeight = heightList.minOrNull() ?: 0
      size.width in minWidth..maxWidth && size.height in minHeight..maxHeight
    }
  }

  fun calculateCorrectRotation(): Int {
    val cameraCharacteristics = camera.cameraCharacteristics
    val sensorOrientation = cameraCharacteristics?.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
    val rotation = windowManager.defaultDisplay.rotation
    var degrees = 0
    when (rotation) {
      Surface.ROTATION_0 -> degrees = 0
      Surface.ROTATION_90 -> degrees = 90
      Surface.ROTATION_180 -> degrees = 180
      Surface.ROTATION_270 -> degrees = 270
    }

    var result: Int
    if (camera.cameraFacing == CameraHelper.Facing.FRONT) {
      result = (sensorOrientation + degrees) % 360
      result = (360 - result) % 360  // Compensate for the mirror effect
    } else {  // Back-facing
      result = (sensorOrientation - degrees + 360) % 360
    }
    return result
  }

  fun addImageListener(
    width: Int, height: Int, format: Int, maxImages: Int, autoClose: Boolean,
    listener: Camera2ApiManager.ImageCallback, framesPerScan: Int, context: Context
  ) {
    camera.addImageListener(width, height, format, maxImages, autoClose, listener, framesPerScan, context)
  }

  fun removeImageListener() {
    camera.removeImageListener()
  }



  fun switchCamera() {
    facing = if (facing == CameraHelper.Facing.BACK) {
      CameraHelper.Facing.FRONT
    } else {
      CameraHelper.Facing.BACK
    }
    if (isRunning()) {
      stop()
      surfaceTexture?.let {
        start(it)
      }
    }
  }

  @Nullable
  fun getCameraCharacteristics(): CameraCharacteristics? {
    return camera.getCameraCharacteristics()
  }

  fun getCameraFacing(): CameraHelper.Facing = facing

  fun getCameraResolutions(facing: CameraHelper.Facing): List<Size> {
    val resolutions = if (facing == CameraHelper.Facing.FRONT) {
      camera.cameraResolutionsFront
    } else {
      camera.cameraResolutionsBack
    }
    return resolutions.toList()
  }

  fun setExposure(level: Int) {
    if (isRunning()) camera.exposure = level
  }

  fun getExposure(): Int {
    return if (isRunning()) camera.exposure else 0
  }

  fun enableLantern() {
    if (isRunning()) camera.enableLantern()
  }

  fun disableLantern() {
    if (isRunning()) camera.disableLantern()
  }

  fun isLanternEnabled(): Boolean {
    return if (isRunning()) camera.isLanternEnabled else false
  }

  fun enableAutoFocus() {
    if (isRunning()) {
      camera.enableAutoFocus()
    }
  }

  fun disableAutoFocus() {
    if (isRunning()) camera.disableAutoFocus()
  }

  fun isAutoFocusEnabled(): Boolean {
    return if (isRunning()) camera.isAutoFocusEnabled else false
  }

  fun tapToFocus(event: MotionEvent) {
    camera.tapToFocus(event)
  }

  fun setZoom(event: MotionEvent) {
    if (isRunning()) camera.setZoom(event)
  }

  fun setZoom(level: Float): Boolean {
    return if (isRunning()) {
      camera.setZoom(level)
    } else {
      false
    }
  }

  fun getZoomRange(): Range<Float> = camera.zoomRange

  fun getZoom(): Float = camera.zoom

  fun enableFaceDetection(callback: FaceDetectorCallback): Boolean {
    return if (isRunning()) camera.enableFaceDetection(callback) else false
  }

  fun disableFaceDetection() {
    if (isRunning()) camera.disableFaceDetection()
  }

  fun isFaceDetectionEnabled() = camera.isFaceDetectionEnabled

  fun camerasAvailable(): Array<String>? = camera.camerasAvailable

  fun getMaxSupportedZoomRatio(): Float {
    return camera.maxSupportedZoomRatio
  }

  fun openCameraId(id: String) {
    if (isRunning()) stop()
    camera.openCameraId(id)
  }

  fun enableOpticalVideoStabilization(): Boolean {
    return if (isRunning()) camera.enableOpticalVideoStabilization() else false
  }

  fun disableOpticalVideoStabilization() {
    if (isRunning()) camera.disableOpticalVideoStabilization()
  }

  fun isOpticalVideoStabilizationEnabled() = camera.isOpticalStabilizationEnabled

  fun enableVideoStabilization(): Boolean {
    return if (isRunning()) camera.enableVideoStabilization() else false
  }

  fun disableVideoStabilization() {
    if (isRunning()) camera.disableVideoStabilization()
  }

  fun isVideoStabilizationEnabled() = camera.isVideoStabilizationEnabled
}