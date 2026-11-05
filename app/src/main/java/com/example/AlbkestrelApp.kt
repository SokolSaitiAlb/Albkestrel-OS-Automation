package com.example

import android.app.Application
import com.example.data.local.AlbkestrelDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.RoutineRepository
import com.example.data.repository.RoutineRepositoryImpl
import com.example.platform.ActionExecutor
import com.example.platform.AutomationScheduler
import com.example.platform.PermissionManager
import com.example.platform.SystemStateMonitor

class AlbkestrelApp : Application() {

    lateinit var database: AlbkestrelDatabase
        private set
    lateinit var repository: RoutineRepository
        private set
    lateinit var preferencesRepository: UserPreferencesRepository
        private set
    lateinit var permissionManager: PermissionManager
        private set
    lateinit var systemStateMonitor: SystemStateMonitor
        private set
    lateinit var actionExecutor: ActionExecutor
        private set
    lateinit var scheduler: AutomationScheduler
        private set

    override fun onCreate() {
        super.onCreate()
        database = AlbkestrelDatabase.getInstance(this)
        repository = RoutineRepositoryImpl(database.routineDao(), database.executionLogDao())
        preferencesRepository = UserPreferencesRepository(this)
        permissionManager = PermissionManager(this)
        systemStateMonitor = SystemStateMonitor(this)
        actionExecutor = ActionExecutor(this)
        scheduler = AutomationScheduler(this)

        // Kick off periodic background worker
        try {
            scheduler.schedulePeriodicWork()
        } catch (_: Exception) {
            // Guard against test environments where WorkManager is not yet configured
        }
    }
}
