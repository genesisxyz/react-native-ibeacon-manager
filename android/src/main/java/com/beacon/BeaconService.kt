package com.beacon

import android.app.*
import android.content.Intent
import android.os.*
import android.util.Log
import com.facebook.react.HeadlessJsTaskService
import com.facebook.react.bridge.ReadableArray
import com.facebook.react.bridge.ReadableMap
import org.altbeacon.beacon.*

class BeaconService : Service(), ServiceInterface {
    private val mBinder: IBinder = ServiceBinder()

    private lateinit var beaconManager: BeaconManager

    inner class ServiceBinder : Binder(), ServiceBinderInterface {
        override val service: BeaconService
            get() = this@BeaconService
    }

    override fun onBind(intent: Intent): IBinder? {
        Log.w(TAG, "onBind")
        return mBinder
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        return START_REDELIVER_INTENT
    }
    override fun stop() {
        stopBeaconScan()
        stopSelf()
    }

    override fun start(notificationId: Int, notification: Notification) {
        startForeground(notificationId, notification)
    }

    override fun onCreate() {
        super.onCreate()

        beaconManager = BeaconManager.getInstanceForApplication(applicationContext)

        val parser = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")
        parser.setHardwareAssistManufacturerCodes(arrayOf(0x004c).toIntArray())

        beaconManager.beaconParsers.add(parser)

        beaconManager.backgroundScanPeriod = 10000
        beaconManager.backgroundBetweenScanPeriod = 0
        beaconManager.foregroundScanPeriod = 10000
        beaconManager.foregroundBetweenScanPeriod = 0
        beaconManager.updateScanPeriods()

        beaconManager.setEnableScheduledScanJobs(false)

        beaconManager.addMonitorNotifier(mMonitorNotifier)
        beaconManager.addRangeNotifier(mRangeNotifier)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        beaconManager.removeMonitorNotifier(mMonitorNotifier)
        beaconManager.removeRangeNotifier(mRangeNotifier)
        super.onDestroy()
    }

    override fun updateOptions(options: HashMap<String, Any>?) {

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

    companion object {
        private const val TAG = "BeaconService"
    }
}