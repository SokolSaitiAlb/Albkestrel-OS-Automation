package com.example.data.local.converter

import com.example.domain.model.ActionType
import com.example.domain.model.DndMode
import com.example.domain.model.RoutineAction
import com.example.domain.model.RoutineCondition
import com.example.domain.model.SystemSettingTarget
import com.example.domain.model.TriggerType
import com.example.domain.model.WifiTriggerState
import org.json.JSONArray
import org.json.JSONObject

object RoutineJsonConverter {

    fun conditionsToJson(conditions: List<RoutineCondition>): String {
        val array = JSONArray()
        for (c in conditions) {
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("type", c.type.name)
            obj.put("isEnabled", c.isEnabled)
            obj.put("hour", c.hour)
            obj.put("minute", c.minute)
            val daysArray = JSONArray()
            c.daysOfWeek.forEach { daysArray.put(it) }
            obj.put("daysOfWeek", daysArray)
            obj.put("batteryThreshold", c.batteryThreshold)
            obj.put("triggerWhenCharging", c.triggerWhenCharging)
            obj.put("wifiState", c.wifiState.name)
            obj.put("targetSsid", c.targetSsid ?: "")
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToConditions(json: String?): List<RoutineCondition> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<RoutineCondition>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val daysArray = obj.optJSONArray("daysOfWeek")
                val days = mutableListOf<Int>()
                if (daysArray != null) {
                    for (d in 0 until daysArray.length()) {
                        days.add(daysArray.getInt(d))
                    }
                }
                list.add(
                    RoutineCondition(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        type = TriggerType.valueOf(obj.getString("type")),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        hour = obj.optInt("hour", 8),
                        minute = obj.optInt("minute", 0),
                        daysOfWeek = if (days.isEmpty()) listOf(1, 2, 3, 4, 5) else days,
                        batteryThreshold = obj.optInt("batteryThreshold", 20),
                        triggerWhenCharging = obj.optBoolean("triggerWhenCharging", false),
                        wifiState = WifiTriggerState.valueOf(obj.optString("wifiState", WifiTriggerState.CONNECTED.name)),
                        targetSsid = obj.optString("targetSsid").takeIf { it.isNotBlank() }
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }

    fun actionsToJson(actions: List<RoutineAction>): String {
        val array = JSONArray()
        for (a in actions) {
            val obj = JSONObject()
            obj.put("id", a.id)
            obj.put("type", a.type.name)
            obj.put("orderIndex", a.orderIndex)
            obj.put("notificationTitle", a.notificationTitle)
            obj.put("notificationBody", a.notificationBody)
            obj.put("targetPackageName", a.targetPackageName)
            obj.put("targetAppLabel", a.targetAppLabel)
            obj.put("dndMode", a.dndMode.name)
            obj.put("settingTarget", a.settingTarget.name)
            array.put(obj)
        }
        return array.toString()
    }

    fun jsonToActions(json: String?): List<RoutineAction> {
        if (json.isNullOrBlank()) return emptyList()
        val list = mutableListOf<RoutineAction>()
        try {
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    RoutineAction(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        type = ActionType.valueOf(obj.getString("type")),
                        orderIndex = obj.optInt("orderIndex", i),
                        notificationTitle = obj.optString("notificationTitle", "Routine Activated"),
                        notificationBody = obj.optString("notificationBody", "Completed"),
                        targetPackageName = obj.optString("targetPackageName", ""),
                        targetAppLabel = obj.optString("targetAppLabel", ""),
                        dndMode = DndMode.valueOf(obj.optString("dndMode", DndMode.PRIORITY_ONLY.name)),
                        settingTarget = try {
                            SystemSettingTarget.valueOf(obj.optString("settingTarget", SystemSettingTarget.BATTERY_SAVER.name))
                        } catch (_: Exception) {
                            SystemSettingTarget.BATTERY_SAVER
                        }
                    )
                )
            }
        } catch (_: Exception) {
        }
        return list
    }
}
