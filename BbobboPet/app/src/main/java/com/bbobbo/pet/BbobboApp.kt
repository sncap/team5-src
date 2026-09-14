package com.bbobbo.pet

import android.app.Application
import com.bbobbo.pet.data.local.AppDatabase
import com.bbobbo.pet.data.repo.PetRepository

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
    }
}
