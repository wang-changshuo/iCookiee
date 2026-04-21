package com.aifinance.app.di

import com.aifinance.app.work.ScheduledRuleWorkScheduler
import com.aifinance.core.data.scheduler.ScheduledRuleScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SchedulerBindingModule {

    @Binds
    @Singleton
    abstract fun bindScheduledRuleScheduler(
        impl: ScheduledRuleWorkScheduler,
    ): ScheduledRuleScheduler
}
