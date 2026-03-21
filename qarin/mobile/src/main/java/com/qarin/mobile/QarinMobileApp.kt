package com.qarin.mobile

import android.app.Application
import com.qarin.mobile.data.db.QarinMobileDatabase

class QarinMobileApp : Application() {
    val database: QarinMobileDatabase by lazy {
        QarinMobileDatabase.getInstance(this)
    }
}
