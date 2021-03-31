package org.openvoipalliance.voiplibexample.logging

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.room.*



@Entity
data class LogEntry(
        @PrimaryKey
        var id:Long?,

        var datetime: String,

        var message: String
)

@Dao
public interface LogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(post: LogEntry)

    @Query("SELECT * FROM LogEntry ORDER BY id DESC")
    fun findAll(): LiveData<List<LogEntry>>
}

@Database(entities = [LogEntry::class], version = 1, exportSchema = false)
abstract class RoomSingleton : RoomDatabase(){
    abstract fun logDao(): LogDao

    companion object{
        private var INSTANCE: RoomSingleton? = null
        fun getInstance(context:Context): RoomSingleton{
            if (INSTANCE == null){
                INSTANCE = Room.databaseBuilder(
                        context,
                        RoomSingleton::class.java,
                        "roomdb")
                        .build()
            }

            return INSTANCE as RoomSingleton
        }
    }
}