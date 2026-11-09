package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.AppSettingDao
import com.example.data.local.dao.CollaboratorDao
import com.example.data.local.dao.GroceryItemDao
import com.example.data.local.dao.PantryItemDao
import com.example.data.local.entity.AppSetting
import com.example.data.local.entity.Collaborator
import com.example.data.local.entity.GroceryItem
import com.example.data.local.entity.PantryItem

@Database(
    entities = [GroceryItem::class, PantryItem::class, Collaborator::class, AppSetting::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groceryItemDao(): GroceryItemDao
    abstract fun pantryItemDao(): PantryItemDao
    abstract fun collaboratorDao(): CollaboratorDao
    abstract fun appSettingDao(): AppSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "freshcart_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
