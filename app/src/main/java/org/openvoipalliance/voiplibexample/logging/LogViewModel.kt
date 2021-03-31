package org.openvoipalliance.voiplibexample.logging

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import org.openvoipalliance.voiplibexample.VoIPLibExampleApplication

class LogViewModel(application: Application): AndroidViewModel(application) {

    private val db:RoomSingleton = RoomSingleton.getInstance(application)

    internal val allLogs : LiveData<List<LogEntry>> = db.logDao().findAll()
}