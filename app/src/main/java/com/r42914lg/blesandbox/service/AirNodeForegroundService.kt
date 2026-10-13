package com.r42914lg.blesandbox.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.r42914lg.blesandbox.MainActivity
import com.r42914lg.blesandbox.R
import com.r42914lg.blesandbox.blewrapper.DeviceConnection
import com.r42914lg.blesandbox.blewrapper.model.DeviceConnectionState
import com.r42914lg.blesandbox.coredata.AirNodeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

private const val CHANNEL_ID = "airnode_status_channel"
private const val ALERT_CHANNEL_ID = "airnode_alerts_channel"

private const val NOTIFICATION_ID = 1001
private const val ALERT_NOTIFICATION_ID = 1002

private const val TEMP_ALERT_THRESHOLD = 30.0f

class AirNodeForegroundService : Service() {

    private val deviceConnection: DeviceConnection by inject()
    private val repository: AirNodeRepository by inject()

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isForegroundStarted = false
    private var isHighTempAlertActive = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        observeState()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeState() {
        serviceScope.launch {
            deviceConnection.connectionState.collect { connState ->
                when (connState) {
                    is DeviceConnectionState.Connected,
                    is DeviceConnectionState.Reconnecting -> {
                        if (!isForegroundStarted) {
                            startForegroundServiceInternal(
                                title = getString(R.string.service_active_title),
                                content = getString(R.string.service_connecting)
                            )
                        }
                    }
                    is DeviceConnectionState.Disconnected,
                    is DeviceConnectionState.Error -> {
                        if (isForegroundStarted) {
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            isForegroundStarted = false
                            stopSelf()
                        }
                    }
                    else -> {}
                }
            }
        }

        serviceScope.launch {
            combine(
                deviceConnection.connectionState,
                repository.observeLatestTemperature(),
                repository.observeLedState()
            ) { connState, temp, led ->
                Triple(connState, temp, led)
            }.collect { (connState, temp, led) ->
                if (isForegroundStarted && connState is DeviceConnectionState.Connected) {
                    val defaultTitle = getString(R.string.app_title)
                    val deviceName = connState.deviceName ?: defaultTitle
                    val currentTemp = temp?.temperatureCelsius ?: 0.0f
                    val tempText = temp?.let { "%.1f °C".format(it.temperatureCelsius) } ?: "--.- °C"
                    val ledText = if (led?.isOn == true) getString(R.string.led_on) else getString(R.string.led_off)
                    val content = "$tempText · $ledText"

                    updateNotification(title = deviceName, content = content)
                    checkTemperatureAlert(currentTemp)
                } else if (isForegroundStarted && connState is DeviceConnectionState.Reconnecting) {
                    val defaultTitle = getString(R.string.app_title)
                    val deviceName = connState.deviceName ?: defaultTitle
                    val titleText = getString(R.string.reconnecting_status, deviceName)
                    val contentText = getString(R.string.signal_lost_reconnecting)
                    updateNotification(title = titleText, content = contentText)
                }
            }
        }
    }

    private fun checkTemperatureAlert(tempCelsius: Float) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        if (tempCelsius > TEMP_ALERT_THRESHOLD) {
            if (!isHighTempAlertActive) {
                isHighTempAlertActive = true
                notificationManager.notify(ALERT_NOTIFICATION_ID, buildAlertNotification(tempCelsius))
            }
        } else {
            if (isHighTempAlertActive) {
                isHighTempAlertActive = false
                notificationManager.cancel(ALERT_NOTIFICATION_ID)
            }
        }
    }

    private fun startForegroundServiceInternal(title: String, content: String) {
        val notification = buildNotification(title, content)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isForegroundStarted = true
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.notify(NOTIFICATION_ID, buildNotification(title, content))
    }

    private fun buildNotification(title: String, content: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildAlertNotification(tempCelsius: Float): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setContentTitle(getString(R.string.alert_title))
            .setContentText(getString(R.string.alert_content, tempCelsius))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // 1. Status channel for live background monitoring
        val statusChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.channel_status_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_status_desc)
            setShowBadge(false)
        }
        manager.createNotificationChannel(statusChannel)

        // 2. High-priority alert channel for temperature warnings (> 30°C)
        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            getString(R.string.channel_alerts_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.channel_alerts_desc)
            enableVibration(true)
            setShowBadge(true)
        }
        manager.createNotificationChannel(alertChannel)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
