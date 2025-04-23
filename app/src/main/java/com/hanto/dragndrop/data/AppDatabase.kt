package com.hanto.dragndrop.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        Category::class,
        Product::class,
        CategoryProductSetup::class
    ],
    version = 1,
    exportSchema = false
)

abstract class AppDatabase : RoomDatabase() {
    abstract fun MainDao(): MainDao
}
