package com.beacon

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.facebook.react.bridge.LifecycleEventListener
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReadableMap

interface ServiceBinderInterface: IBinder {
    val service: ServiceInterface
}

interface ServiceInterface {
    fun updateOptions(options: HashMap<String, Any>?)
    // fun setParentContext(context: Context?)
    fun start(notificationId: Int, notification: Notification)
    fun stop()
}

class ConnectService(private val reactContext: ReactApplicationContext, private val serviceClass: Class<*>): LifecycleEventListener {
    companion object {
        private const val TAG = "ConnectService"
    }

    private var mBound = false

    var mService: ServiceInterface? = null
        private set

    private var mStartPromise: Promise? = null

    var intentService: Intent? = null

    var notificationManager = NotificationManager(reactContext)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(className: ComponentName,
                                        service: IBinder) {
            Log.i(TAG, "Intent service started")
            Log.i(TAG, "onServiceConnected")

            mService = (service as ServiceBinderInterface).service
            mService!!.start(notificationManager.notificationId, notificationManager.notification)
            mBound = true
            mStartPromise?.resolve(true)
            mStartPromise = null
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            // TODO: why is this never called?
            Log.i(TAG, "onServiceDisconnected")
            mBound = false
        }
    }

    fun setOptions(options: ReadableMap) {
        notificationManager.updateNotification(options.toHashMap())
        mService?.updateOptions(options.toHashMap())

        if (mBound) {
            notificationManager.showNotification()
        }
    }

    fun startService(promise: Promise) {
        if (intentService == null) {
            intentService = Intent(reactContext, serviceClass)
            mStartPromise = promise
            reactContext.bindService(intentService, connection, Context.BIND_AUTO_CREATE)
            reactContext.startService(intentService)
        } else {
            Log.w(TAG, "Intent service is running")
            promise.resolve(true)
        }
    }

    fun stopService(promise: Promise) {
        if (intentService != null) {
            mService?.stop();
            reactContext.unbindService(connection)
            reactContext.stopService(intentService)
            mBound = false
            mService = null
            intentService = null
            Log.i(TAG, "Intent service stopped")
        } else {
            Log.w(TAG, "Intent service is not running")
        }
        promise.resolve(true)
    }

    override fun onHostResume() {
        Log.i(TAG, "onHostResume")
        if (intentService != null) { // TODO: can't check mBound, onServiceDisconnected never called
            reactContext.bindService(intentService, connection, Context.BIND_AUTO_CREATE)
            mBound = true // TODO: shouldn't be required
        }
    }

    override fun onHostPause() {
        Log.i(TAG, "onHostPause")
        if (mBound) {
            reactContext.unbindService(connection)
            mBound = false
        }
    }

    override fun onHostDestroy() {
        Log.i(TAG, "onHostDestroy")
    }
}
