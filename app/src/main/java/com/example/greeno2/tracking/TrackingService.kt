package com.example.greeno2.tracking

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.greeno2.MainActivity
import com.example.greeno2.R
import com.example.greeno2.appContainer
import com.example.greeno2.data.TransportMode
import com.example.greeno2.domain.DistanceAccumulator
import com.example.greeno2.domain.GeoPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Records a trip with the fused location provider while running as a foreground service, so
 * tracking continues with the screen off. On stop it scores and saves the trip itself, which
 * means a trip is saved even if the user stops it from the notification with the app closed.
 */
class TrackingService : Service() {

    private lateinit var fused: FusedLocationProviderClient
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var accumulator = DistanceAccumulator()
    private var current: LiveTrip? = null
    private var lastNotificationUpdate = 0L
    private var stopping = false

    private val callback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.locations.forEach { onLocation(it) }
        }
    }

    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        createChannel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val mode = TransportMode.fromName(intent.getStringExtra(EXTRA_MODE))
                val userId = intent.getLongExtra(EXTRA_USER_ID, -1L)
                startTracking(mode, userId)
            }
            ACTION_STOP -> stopTracking()
        }
        return START_NOT_STICKY
    }

    @SuppressLint("MissingPermission") // checked by hasLocationPermission()
    private fun startTracking(mode: TransportMode, userId: Long) {
        if (current != null) return // already recording
        if (userId <= 0 || !hasLocationPermission()) {
            stopSelf()
            return
        }
        val trip = LiveTrip(userId = userId, mode = mode, startTimeMs = System.currentTimeMillis())
        current = trip
        accumulator = DistanceAccumulator()
        TrackingStore.update(trip)

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(trip), type)

        // Faster modes need more frequent fixes; walking can save battery.
        val intervalMs = when (mode) {
            TransportMode.WALK -> 5_000L
            TransportMode.BIKE, TransportMode.E_BIKE, TransportMode.E_SCOOTER -> 3_000L
            else -> 4_000L
        }
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMs)
            .setMinUpdateIntervalMillis(intervalMs / 2)
            .setMinUpdateDistanceMeters(3f)
            .build()
        try {
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        } catch (e: SecurityException) {
            Log.w(TAG, "Location permission revoked", e)
            stopTracking()
        }
    }

    private fun onLocation(location: Location) {
        val trip = current ?: return
        val point = GeoPoint(location.latitude, location.longitude, location.accuracy, location.time)
        val accepted = accumulator.add(point)
        val updated = trip.copy(
            distanceMeters = accumulator.distanceMeters,
            currentSpeedKmh = if (location.hasSpeed()) location.speed * 3.6 else accumulator.lastSpeedKmh,
            gpsFixes = trip.gpsFixes + 1,
            path = if (accepted) (trip.path + (location.latitude to location.longitude)).takeLast(MAX_PATH_POINTS) else trip.path,
        )
        current = updated
        TrackingStore.update(updated)

        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdate > 5_000) {
            lastNotificationUpdate = now
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildNotification(updated))
        }
    }

    private fun stopTracking() {
        if (stopping) return
        stopping = true
        fused.removeLocationUpdates(callback)
        val trip = current
        if (trip == null) {
            finish()
            return
        }
        val endTime = System.currentTimeMillis()
        val repository = appContainer.repository
        TrackingStore.setSaving(true)
        scope.launch {
            try {
                val saved = repository.saveTrip(
                    userId = trip.userId,
                    mode = trip.mode,
                    startTime = trip.startTimeMs,
                    endTime = endTime,
                    distanceMeters = accumulator.distanceMeters,
                    maxSegmentSpeedKmh = accumulator.maxSegmentSpeedKmh,
                )
                TrackingStore.complete(saved)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save trip", e)
                TrackingStore.update(null)
            } finally {
                TrackingStore.setSaving(false)
                current = null
                finish()
            }
        }
    }

    private fun finish() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        fused.removeLocationUpdates(callback)
        if (current != null && !stopping) TrackingStore.update(null)
        scope.cancel()
        super.onDestroy()
    }

    private fun hasLocationPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun createChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply { description = getString(R.string.tracking_channel_desc) }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildNotification(trip: LiveTrip): Notification {
        val openApp = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stop = PendingIntent.getService(
            this, 1,
            Intent(this, TrackingService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val km = "%.2f km".format(trip.distanceMeters / 1000.0)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_leaf)
            .setContentTitle(getString(R.string.tracking_notification_title, trip.mode.label.lowercase()))
            .setContentText("${trip.mode.emoji} $km so far")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.tracking_stop), stop)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    companion object {
        private const val TAG = "TrackingService"
        private const val CHANNEL_ID = "trip_tracking"
        private const val NOTIFICATION_ID = 42
        private const val MAX_PATH_POINTS = 2_000
        const val ACTION_START = "com.example.greeno2.action.START_TRIP"
        const val ACTION_STOP = "com.example.greeno2.action.STOP_TRIP"
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_USER_ID = "user_id"

        fun start(context: Context, mode: TransportMode, userId: Long) {
            val intent = Intent(context, TrackingService::class.java)
                .setAction(ACTION_START)
                .putExtra(EXTRA_MODE, mode.name)
                .putExtra(EXTRA_USER_ID, userId)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.startService(Intent(context, TrackingService::class.java).setAction(ACTION_STOP))
        }
    }
}
