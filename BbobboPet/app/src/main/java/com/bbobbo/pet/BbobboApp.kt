package com.bbobbo.pet

import android.app.Application
import com.bbobbo.pet.data.local.AppDatabase
import com.bbobbo.pet.data.repo.PetRepository
import com.bbobbo.pet.notify.CareCheckWorker
import com.bbobbo.pet.notify.Notifier

/**
 * DI 프레임워크 없이 단순 서비스 로케이터로 구성.
 * 규모가 커지면 Hilt 로 교체 (기획서 T-01).
 */
class BbobboApp : Application() {
    lateinit var repository: PetRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = PetRepository(AppDatabase.get(this))
        Notifier.ensureChannel(this)
        // 알림 권한이 없으면 Worker 가 스스로 아무것도 하지 않으므로 항상 예약해둔다.
        CareCheckWorker.schedule(this)
    }
}
