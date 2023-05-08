package com.beacon

import android.content.Intent
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
import com.facebook.react.jstasks.HeadlessJsTaskConfig

class BeaconEventService : HeadlessJsTaskService() {
  override fun getTaskConfig(intent: Intent): HeadlessJsTaskConfig? {
    val extras = intent.extras

    return HeadlessJsTaskConfig(
      "Beacons",
      if (extras != null) Arguments.fromBundle(extras) else Arguments.createMap(),
      0L,
      true)
  }

  companion object {
    private const val TAG = "BeaconEventService"
  }
}
