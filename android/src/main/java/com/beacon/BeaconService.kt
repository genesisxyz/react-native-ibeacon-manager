package com.beacon

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.Arguments
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

    beaconManager.backgroundScanPeriod = 1100
    beaconManager.backgroundBetweenScanPeriod = 0
    beaconManager.foregroundScanPeriod = 1100
    beaconManager.foregroundBetweenScanPeriod = 0
    beaconManager.updateScanPeriods()

    beaconManager.addMonitorNotifier(mMonitorNotifier)
    beaconManager.addRangeNotifier(mRangeNotifier)

    val handlerThread = HandlerThread(TAG)
    handlerThread.start()
    mServiceHandler = Handler(handlerThread.looper)
  }

  override fun onDestroy() {
    Log.d(TAG, "onDestroy")
    mServiceHandler?.removeCallbacksAndMessages(null)
    beaconManager.removeMonitorNotifier(mMonitorNotifier)
    beaconManager.removeRangeNotifier(mRangeNotifier)
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
    Log.d(TAG, "startBeaconScan")
    if (this.regions.isNotEmpty()) {
      this.stopBeaconScan()
    }

    for (i in 0 until beacons.size()) {
      val beaconMap: ReadableMap = beacons.getMap(i)

      val id = beaconMap.getString("id")!!
      val uuid = beaconMap.getString("uuid")!!
      val major = if (beaconMap.hasKey("major")) beaconMap.getInt("major") else null
      val minor = if (beaconMap.hasKey("minor")) beaconMap.getInt("minor") else null

      val region = Region(
        id,
        Identifier.parse(uuid),
        if (major != null) Identifier.fromInt(major) else null,
        if (minor != null) Identifier.fromInt(minor) else null,
      )

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

      val beaconsArray = mutableListOf<Bundle>()

      val beaconsMap = Bundle()

      beaconsMap.putString("uuid", region.id1.toString())
      if (region.id2 !== null) {
        beaconsMap.putInt("major", region.id2.toInt())
      }
      if (region.id3 !== null) {
        beaconsMap.putInt("minor", region.id3.toInt())
      }

      beaconsArray.add(beaconsMap)

      val extra = Bundle()
      extra.putParcelableArray("beacons", beaconsArray.toTypedArray())

      val myIntent = Intent(applicationContext, BeaconEventService::class.java)
      myIntent.putExtra("data", extra)

      applicationContext.startService(myIntent)
      HeadlessJsTaskService.acquireWakeLockNow(applicationContext)
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

    if (beacons.isEmpty()) return@RangeNotifier;

    val beaconsArray = mutableListOf<Bundle>()

    for (beacon: Beacon in beacons) {
      Log.d(BeaconModule.TAG, "$beacon about ${beacon.distance} meters away")

      val beaconsMap = Bundle()

      beaconsMap.putString("uuid", beacon.id1.toString())
      beaconsMap.putInt("major", beacon.id2.toInt())
      beaconsMap.putInt("minor", beacon.id3.toInt())
      beaconsMap.putDouble("distance", beacon.distance)

      beaconsArray.add(beaconsMap)
    }

    val extra = Bundle()
    extra.putParcelableArray("beacons", beaconsArray.toTypedArray())

    val myIntent = Intent(applicationContext, BeaconEventService::class.java)
    myIntent.putExtra("data", extra)

    applicationContext.startService(myIntent)
    HeadlessJsTaskService.acquireWakeLockNow(applicationContext)
  }
}
