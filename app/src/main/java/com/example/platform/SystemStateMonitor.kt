package com.example.platform

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import com.example.domain.model.SystemEvaluationContext
import java.util.Calendar

class SystemStateMonitor(private val context: Context) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    private val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

    fun getBatteryInfo(): Pair<Int, Boolean> {
        val batteryIntent = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1

        val percentage = if (level >= 0 && scale > 0) {
            ((level / scale.toFloat()) * 100).toInt()
        } else {
            50
        }

        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        return Pair(percentage, isCharging)
    }

    fun isWifiConnected(): Boolean {
        val activeNetwork = connectivityManager?.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun getWifiSsid(): String? {
        return try {
            val info = wifiManager?.connectionInfo
            val ssid = info?.ssid?.replace("\"", "")
            if (ssid == null || ssid == "<unknown ssid>" || ssid.isBlank()) null else ssid
        } catch (_: Exception) {
            null
        }
    }

    fun getCurrentEvaluationContext(isManual: Boolean = false): SystemEvaluationContext {
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(Calendar.MINUTE)
        // Calendar: Sunday=1, Monday=2 .. Saturday=7.
        // ISO-8601: Monday=1 .. Sunday=7
        val calDay = calendar.get(Calendar.DAY_OF_WEEK)
        val isoDay = if (calDay == Calendar.SUNDAY) 7 else calDay - 1

        val (batteryPercentage, isCharging) = getBatteryInfo()
        val wifiConnected = isWifiConnected()
        val wifiSsid = getWifiSsid()

        return SystemEvaluationContext(
            currentTimestamp = System.currentTimeMillis(),
            currentHour = currentHour,
            currentMinute = currentMinute,
            currentDayOfWeek = isoDay,
            minuteTolerance = 3,
            batteryPercentage = batteryPercentage,
            isBatteryCharging = isCharging,
            isWifiConnected = wifiConnected,
            currentWifiSsid = wifiSsid,
            isManualInvocation = isManual
        )
    }
}
