package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.ExecutionLogDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.entity.ExecutionLogEntity
import com.example.data.local.entity.RoutineEntity
import com.example.domain.model.ActionType
import com.example.domain.model.DndMode
import com.example.domain.model.MatchCondition
import com.example.domain.model.Routine
import com.example.domain.model.RoutineAction
import com.example.domain.model.RoutineCondition
import com.example.domain.model.SystemSettingTarget
import com.example.domain.model.TriggerType
import com.example.domain.model.WifiTriggerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [RoutineEntity::class, ExecutionLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AlbkestrelDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao
    abstract fun executionLogDao(): ExecutionLogDao

    companion object {
        @Volatile
        private var INSTANCE: AlbkestrelDatabase? = null

        fun getInstance(context: Context): AlbkestrelDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AlbkestrelDatabase::class.java,
                    "albkestrel_automation.db"
                ).addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Prepopulate default presets on first creation
                        CoroutineScope(Dispatchers.IO).launch {
                            val routineDao = getInstance(context).routineDao()
                            createDefaultPresets().forEach { preset ->
                                routineDao.insertRoutine(RoutineEntity.fromDomain(preset))
                            }
                        }
                    }
                }).build()
                INSTANCE = instance
                instance
            }
        }

        fun createDefaultPresets(): List<Routine> {
            return listOf(
                Routine(
                    id = 0,
                    name = "Sleep Mode",
                    description = "Wind down at night, silences distractions and alerts",
                    icon = "sleep",
                    isEnabled = true,
                    matchMode = MatchCondition.ALL,
                    conditions = listOf(
                        RoutineCondition(
                            type = TriggerType.SCHEDULE,
                            hour = 23,
                            minute = 0,
                            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7)
                        )
                    ),
                    actions = listOf(
                        RoutineAction(
                            type = ActionType.DO_NOT_DISTURB,
                            dndMode = DndMode.PRIORITY_ONLY,
                            orderIndex = 0
                        ),
                        RoutineAction(
                            type = ActionType.NOTIFICATION,
                            notificationTitle = "Sleep Mode Active",
                            notificationBody = "Do Not Disturb activated. Good night!",
                            orderIndex = 1
                        )
                    )
                ),
                Routine(
                    id = 0,
                    name = "Work Focus",
                    description = "Mute distractions during weekday business hours",
                    icon = "work",
                    isEnabled = false,
                    matchMode = MatchCondition.ALL,
                    conditions = listOf(
                        RoutineCondition(
                            type = TriggerType.SCHEDULE,
                            hour = 9,
                            minute = 0,
                            daysOfWeek = listOf(1, 2, 3, 4, 5)
                        )
                    ),
                    actions = listOf(
                        RoutineAction(
                            type = ActionType.DO_NOT_DISTURB,
                            dndMode = DndMode.PRIORITY_ONLY,
                            orderIndex = 0
                        ),
                        RoutineAction(
                            type = ActionType.NOTIFICATION,
                            notificationTitle = "Work Mode Active",
                            notificationBody = "Distractions silenced for focus time",
                            orderIndex = 1
                        )
                    )
                ),
                Routine(
                    id = 0,
                    name = "Home Arrived",
                    description = "Triggered when connected to home Wi-Fi",
                    icon = "home",
                    isEnabled = false,
                    matchMode = MatchCondition.ALL,
                    conditions = listOf(
                        RoutineCondition(
                            type = TriggerType.WIFI,
                            wifiState = WifiTriggerState.CONNECTED
                        )
                    ),
                    actions = listOf(
                        RoutineAction(
                            type = ActionType.NOTIFICATION,
                            notificationTitle = "Welcome Home",
                            notificationBody = "Connected to Wi-Fi. Home routines ready.",
                            orderIndex = 0
                        )
                    )
                ),
                Routine(
                    id = 0,
                    name = "Battery Saver",
                    description = "Guide to enable Battery Saver when power drops below 20%",
                    icon = "battery",
                    isEnabled = true,
                    matchMode = MatchCondition.ALL,
                    conditions = listOf(
                        RoutineCondition(
                            type = TriggerType.BATTERY,
                            batteryThreshold = 20,
                            triggerWhenCharging = false
                        )
                    ),
                    actions = listOf(
                        RoutineAction(
                            type = ActionType.NOTIFICATION,
                            notificationTitle = "Low Battery Detected (≤20%)",
                            notificationBody = "Tap to review battery optimization settings",
                            orderIndex = 0
                        ),
                        RoutineAction(
                            type = ActionType.BATTERY_SAVER_GUIDE,
                            settingTarget = SystemSettingTarget.BATTERY_SAVER,
                            orderIndex = 1
                        )
                    )
                ),
                Routine(
                    id = 0,
                    name = "Gaming Mode",
                    description = "Silence non-essential alerts for gaming sessions",
                    icon = "game",
                    isEnabled = false,
                    matchMode = MatchCondition.ALL,
                    conditions = listOf(
                        RoutineCondition(
                            type = TriggerType.MANUAL
                        )
                    ),
                    actions = listOf(
                        RoutineAction(
                            type = ActionType.DO_NOT_DISTURB,
                            dndMode = DndMode.ALARMS_ONLY,
                            orderIndex = 0
                        ),
                        RoutineAction(
                            type = ActionType.NOTIFICATION,
                            notificationTitle = "Gaming Mode Engaged",
                            notificationBody = "Alarms-only Do Not Disturb mode enabled",
                            orderIndex = 1
                        )
                    )
                )
            )
        }
    }
}
