package com.qarin.wear

import android.app.Application
import com.qarin.wear.data.db.QarinWearDatabase

class QarinApp : Application() {
    val database: QarinWearDatabase by lazy {
        QarinWearDatabase.getInstance(this)
    }
}
