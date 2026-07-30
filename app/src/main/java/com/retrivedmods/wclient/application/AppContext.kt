package com.retrivedmods.wclient.application

import android.app.Application

class AppContext : Application() {

    companion object {
        lateinit var instance: AppContext
            private set

        val isInitialized: Boolean
            get() = ::instance.isInitialized
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}