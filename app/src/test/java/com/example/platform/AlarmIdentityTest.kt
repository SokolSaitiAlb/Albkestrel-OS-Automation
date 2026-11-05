package com.example.platform

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.Routine
import com.example.domain.model.RoutineCondition
import com.example.domain.model.TriggerType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AlarmIdentityTest {

    @Test
    fun `alarm request code is deterministic and positive`() {
        val routineId = 42L
        val slot0Code1 = AutomationScheduler.getAlarmRequestCode(routineId, 0)
        val slot0Code2 = AutomationScheduler.getAlarmRequestCode(routineId, 0)
        val slot1Code = AutomationScheduler.getAlarmRequestCode(routineId, 1)
        val otherRoutineCode = AutomationScheduler.getAlarmRequestCode(43L, 0)

        // Deterministic
        assertEquals(slot0Code1, slot0Code2)
        assertTrue(slot0Code1 >= 0)

        // Distinct across slots and routines
        assertNotEquals(slot0Code1, slot1Code)
        assertNotEquals(slot0Code1, otherRoutineCode)
    }

    @Test
    fun `alarm intent has stable action, component and deterministic data URI`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val routineId = 15L
        val slot = 0

        val intent1 = AutomationScheduler.createAlarmIntent(context, routineId, slot)
        val intent2 = AutomationScheduler.createAlarmIntent(context, routineId, slot)
        val differentSlotIntent = AutomationScheduler.createAlarmIntent(context, routineId, slot + 1)

        // Intent filterEquals must match for identical routineId and slot
        assertTrue(intent1.filterEquals(intent2))
        assertEquals("albkestrel://routine/15/slot/0", intent1.dataString)
        assertEquals(AutomationScheduler.ACTION_ROUTINE_ALARM, intent1.action)

        // Intent for different slot must NOT filterEqual
        assertNotEquals(intent1.dataString, differentSlotIntent.dataString)
        assertTrue(!intent1.filterEquals(differentSlotIntent))
    }

    @Test
    fun `calculateNextTriggerTime schedules future timestamp on selected days`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val scheduler = AutomationScheduler(context)

        // Schedule at 23:59 every day
        val nextTime = scheduler.calculateNextTriggerTime(
            hour = 23,
            minute = 59,
            daysOfWeek = listOf(1, 2, 3, 4, 5, 6, 7)
        )

        assertTrue(nextTime > System.currentTimeMillis())

        // Calculate time on calendar and verify hour and minute
        val cal = Calendar.getInstance().apply { timeInMillis = nextTime }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, cal.get(Calendar.MINUTE))
        assertEquals(0, cal.get(Calendar.SECOND))
    }
}
