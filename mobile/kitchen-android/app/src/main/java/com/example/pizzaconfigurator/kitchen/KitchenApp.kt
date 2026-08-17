package com.example.pizzaconfigurator.kitchen

import android.app.Application
import com.example.pizzaconfigurator.kitchen.di.AppContainer

class KitchenApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
