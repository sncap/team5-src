package com.bbobbo.pet.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.bbobbo.pet.data.local.AppDatabase
import com.bbobbo.pet.data.prefs.SettingsStore
import com.bbobbo.pet.data.repo.PetRepository
import com.bbobbo.pet.domain.NotifyKind
import com.bbobbo.pet.domain.StatEngine
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * 기획서 T-39. 주기적으로 깨어나 오프라인 정산 결과를 보고 알림을 띄운다.
 * 실시간 타이머 대신 §1-1 의 "마지막 접속 기준 일괄 정산"을 그대로 재사용하므로
 * 앱이 떠 있지 않아도 화면에 보이는 것과 같은 스탯을 기준으로 판단한다.
 */
class CareCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!Notifier.hasPermission(applicationContext)) return Result.success()

        val settings = SettingsStore(applicationContext).flow.first()
        val db = AppDatabase.get(applicationContext)
        val repo = PetRepository(db)
        val state = db.petDao().get() ?: return Result.success()

        // DB 는 건드리지 않고 "지금 시점의 스탯"만 계산한다. 저장은 앱이 켜질 때 한다.
        val projected = StatEngine.applyElapsed(state).state

        if (settings.notifyEnabled(NotifyKind.HUNGRY) && projected.fullness <= 30f) {
            Notifier.notify(applicationContext, NotifyKind.HUNGRY, "뽀뽀가 배고파해요 🍚")
        }
        if (settings.notifyEnabled(NotifyKind.DIRTY) && projected.clean <= 30f) {
            Notifier.notify(applicationContext, NotifyKind.DIRTY, "뽀뽀가 목욕하고 싶대요 🛁")
        }
        if (settings.notifyEnabled(NotifyKind.ENERGY) && projected.energy >= 100f && state.energy < 100f) {
            Notifier.notify(
                applicationContext, NotifyKind.ENERGY,
                "에너지가 가득 찼어요! 미니게임 하러 갈까요?"
            )
        }

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (settings.notifyEnabled(NotifyKind.ATTENDANCE) && hour == 20 &&
            repo.todayAttendance() == null
        ) {
            Notifier.notify(applicationContext, NotifyKind.ATTENDANCE, "오늘 뽀뽀 만나러 안 왔어요… 🥺")
        }

        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "bbobbo_care_check"

        /**
         * WorkManager 의 최소 주기가 15분이라 그보다 자주는 못 깨운다.
         * 스탯은 시간당 최대 5 감소이므로 1시간 주기로도 "30 이하" 판정에 충분하다.
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<CareCheckWorker>(1, TimeUnit.HOURS)
                .setInitialDelay(30, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
