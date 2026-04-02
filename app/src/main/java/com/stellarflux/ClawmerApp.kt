package com.stellarflux

import android.app.Application
import com.stellarflux.data.remote.OpenClawClient
import com.stellarflux.data.repository.OpenClawRepository
import com.stellarflux.data.repository.ProjectRepository
import com.stellarflux.data.repository.ServerRepository

class ClawmerApp : Application() {

    lateinit var serverRepository: ServerRepository
        private set
    lateinit var projectRepository: ProjectRepository
        private set
    lateinit var openClawRepository: OpenClawRepository
        private set

    override fun onCreate() {
        super.onCreate()
        serverRepository = ServerRepository(this)
        projectRepository = ProjectRepository(this)
        openClawRepository = OpenClawRepository()
    }
}
