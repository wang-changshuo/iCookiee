package com.aifinance.app

import android.app.Application
import com.aifinance.core.data.scheduler.ScheduledRuleScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AiFinanceApplication : Application() {

    @Inject
    lateinit var scheduledRuleScheduler: ScheduledRuleScheduler

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            scheduledRuleScheduler.rescheduleAllEnabled()
        }
    }
}
