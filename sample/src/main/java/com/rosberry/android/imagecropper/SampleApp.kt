package com.rosberry.android.imagecropper

import android.app.Application
import com.rosberry.android.imagecropper.gallery.GalleryViewModel
import com.rosberry.android.localmediaprovider.MediaProvider
import org.koin.android.ext.koin.androidContext
import org.koin.android.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.dsl.module

class SampleApp : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@SampleApp)
            modules(appModule)
        }
    }

    val appModule = module {
        single { MediaProvider(get()) }
        viewModel { GalleryViewModel(get()) }
    }
}