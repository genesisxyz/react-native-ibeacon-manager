package com.beacon

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import org.altbeacon.beacon.*

class BeaconService: Service(), ServiceInterface {

  companion object {
    private const val TAG = "BeaconService"
  }

  private var mServiceHandler: Handler? = null

  private val binder: IBinder = UpdatesBinder()

  private var isBound = false

  private lateinit var beaconManager: BeaconManager

  inner class UpdatesBinder() : Binder(), ServiceBinderInterface {
    // Return this instance of LocalService so clients can call public methods
    override val service: BeaconService
      get() =// Return this instance of LocalService so clients can call public methods
        this@BeaconService
  }


  // region binding

  override fun onBind(intent: Intent): IBinder {
    Log.i(TAG, "onBind")

    isBound = true

    return binder
  }


  override fun onUnbind(intent: Intent?): Boolean {
    super.onUnbind(intent)

    Log.i(TAG, "onUnbind")

    isBound = false

    return true
  }


  override fun onRebind(intent: Intent?) {
    super.onRebind(intent)

    Log.i(TAG, "onRebind")

    isBound = true
  }

  // endregion

  // region lifecycle

  override fun onCreate() {
    super.onCreate()

    beaconManager = BeaconManager.getInstanceForApplication(applicationContext)
    beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24"))

    this.enableBackgroundService()

    beaconManager.backgroundScanPeriod = 1000
    beaconManager.backgroundBetweenScanPeriod = 1000
    beaconManager.foregroundScanPeriod = 1000
    beaconManager.foregroundBetweenScanPeriod = 1000
    beaconManager.updateScanPeriods()

    beaconManager.addMonitorNotifier(mMonitorNotifier)
    beaconManager.addRangeNotifier(mRangeNotifier)

    val handlerThread = HandlerThread(TAG)
    handlerThread.start()
    mServiceHandler = Handler(handlerThread.looper)
  }

  override fun onDestroy() {

    mServiceHandler?.removeCallbacksAndMessages(null)
    super.onDestroy()
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    startForeground()
    return START_NOT_STICKY
  }

  // endregion

  private fun startForeground() {
    Notification.createNotificationChannel(applicationContext)
    val notification = Notification.getNotification(applicationContext)
    startForeground(Notification.notificationId, notification)
  }

  // region ServiceInterface

  override fun updateOptions(options: HashMap<String, Any>?) {

  }

  // endregion

  private fun enableBackgroundService() {
    beaconManager.enableForegroundServiceScanning(Notification.getNotification(applicationContext), Notification.notificationId)
    beaconManager.setEnableScheduledScanJobs(false)
  }

  private val regions = mutableListOf<Region>()

  fun startBeaconScan(beacons: ReadableArray) {
    if (this.regions.isNotEmpty()) {
      this.stopBeaconScan()
    }

    for (i in 0 until beacons.size()) {
      val beaconMap: ReadableMap = beacons.getMap(i)

      val id = beaconMap.getString("id")!!
      val uuid = beaconMap.getString("uuid")!!
      val minor = beaconMap.getInt("minor")
      val major = beaconMap.getInt("major")

      val region = Region(id, Identifier.parse(uuid), Identifier.fromInt(minor), Identifier.fromInt(major))

      regions.add(region)

      beaconManager.startMonitoring(region)
    }
  }

  fun stopBeaconScan() {
    regions.forEach {
      beaconManager.stopMonitoring(it)
      beaconManager.stopRangingBeacons(it)
    }

    regions.clear()
  }

  private val mMonitorNotifier: MonitorNotifier = object : MonitorNotifier {
    override fun didEnterRegion(region: Region) {

      Log.d(BeaconModule.TAG, "BEACON didEnterRegion")
      beaconManager.startRangingBeacons(region)
    }

    override fun didExitRegion(region: Region) {

      Log.d(BeaconModule.TAG, "BEACON didExitRegion")
      beaconManager.stopRangingBeacons(region)
    }

    override fun didDetermineStateForRegion(i: Int, region: Region) {
      var state = "unknown"
      when (i) {
        MonitorNotifier.INSIDE -> state = "inside"
        MonitorNotifier.OUTSIDE -> state = "outside"
        else -> {}
      }
    }
  }

  private val mRangeNotifier: RangeNotifier = RangeNotifier { beacons, region ->
    Log.d(BeaconModule.TAG, "Ranged: ${beacons.count()} beacons")
    for (beacon: Beacon in beacons) {
      Log.d(BeaconModule.TAG, "$beacon about ${beacon.distance} meters away")

      val myIntent = Intent(applicationContext, BeaconEventService::class.java)
      myIntent.putExtra("uuid", beacon.id1.toString())
      myIntent.putExtra("distance", beacon.distance)

      applicationContext.startService(myIntent)
      HeadlessJsTaskService.acquireWakeLockNow(applicationContext)
    }
  }
}
