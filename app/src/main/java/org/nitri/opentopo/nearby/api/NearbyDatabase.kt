package org.nitri.opentopo.nearby.api

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

import org.nitri.opentopo.nearby.da.NearbyDao
import org.nitri.opentopo.nearby.entity.NearbyItem

@Database(entities = [NearbyItem::class], version = 1, exportSchema = false)
abstract class NearbyDatabase : RoomDatabase() {

    abstract fun nearbyDao(): NearbyDao

    companion object {

        private var instance: NearbyDatabase? = null

        @Synchronized
        fun getDatabase(context: Context): NearbyDatabase {
            if (instance == null) {
                instance = Room.databaseBuilder(context.applicationContext, NearbyDatabase::class.java, "reading-database")
                        .build()
            }
            return instance!!
        }
    }

}
