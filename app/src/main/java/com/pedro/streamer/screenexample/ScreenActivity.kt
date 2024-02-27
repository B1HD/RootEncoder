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
package com.pedro.streamer.screenexample

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.pedro.common.ConnectChecker
import com.pedro.streamer.R

/**
 * More documentation see:
 * [com.pedro.library.base.DisplayBase]
 * [com.pedro.library.rtmp.RtmpDisplay]
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class ScreenActivity : AppCompatActivity(), ConnectChecker {

  private lateinit var button: ImageView
  private lateinit var etUrl: EditText

  private val activityResultContract = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
    val data = result.data
    if (data != null && result.resultCode == RESULT_OK) {
      val screenService = ScreenService.INSTANCE
      if (screenService != null) {
        val endpoint = etUrl.text.toString()
        screenService.prepareStream(result.resultCode, data)
        screenService.startStream(endpoint)
      }
    } else {
      Toast.makeText(this, "No permissions available", Toast.LENGTH_SHORT).show()
      button.setImageResource(R.drawable.stream_icon)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    setContentView(R.layout.activity_display)
    button = findViewById(R.id.b_start_stop)
    etUrl = findViewById(R.id.et_rtp_url)
    val screenService = ScreenService.INSTANCE
    //No streaming/recording start service
    if (screenService == null) {
      startService(Intent(this, ScreenService::class.java))
    }
    if (screenService != null && screenService.isStreaming()) {
      button.setImageResource(R.drawable.stream_stop_icon)
    } else {
      button.setImageResource(R.drawable.stream_icon)
    }
    button.setOnClickListener {
      val service = ScreenService.INSTANCE
      if (service != null) {
        service.setCallback(this)
        if (!service.isStreaming()) {
          button.setImageResource(R.drawable.stream_stop_icon)
          activityResultContract.launch(service.sendIntent())
        } else {
          stopStream()
        }
      }
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    val screenService = ScreenService.INSTANCE
    if (screenService != null && !screenService.isStreaming() && !screenService.isRecording()) {
      screenService.setCallback(null)
      activityResultContract.unregister()
      //stop service only if no streaming or recording
      stopService(Intent(this, ScreenService::class.java))
    }
  }

  private fun stopStream() {
    val screenService = ScreenService.INSTANCE
    screenService?.stopStream()
    button.setImageResource(R.drawable.stream_icon)
  }

  override fun onConnectionStarted(url: String) {}

  override fun onConnectionSuccess() {
    Toast.makeText(this@ScreenActivity, "Connected", Toast.LENGTH_SHORT).show()
  }

  override fun onConnectionFailed(reason: String) {
    Handler(Looper.getMainLooper()).post {
      stopStream()
      Toast.makeText(this@ScreenActivity, "Failed: $reason", Toast.LENGTH_LONG)
        .show()
    }
  }

  override fun onNewBitrate(bitrate: Long) {}
  override fun onDisconnect() {
    Toast.makeText(this@ScreenActivity, "Disconnected", Toast.LENGTH_SHORT).show()
  }

  override fun onAuthError() {
    Handler(Looper.getMainLooper()).post {
      stopStream()
      Toast.makeText(this@ScreenActivity, "Auth error", Toast.LENGTH_LONG).show()
    }
  }

  override fun onAuthSuccess() {
    Toast.makeText(this@ScreenActivity, "Auth success", Toast.LENGTH_SHORT).show()
  }
}
