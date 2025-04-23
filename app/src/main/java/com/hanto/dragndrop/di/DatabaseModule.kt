package com.hanto.dragndrop.di

import android.content.Context
import androidx.room.Room
import com.hanto.dragndrop.data.AppDatabase
import com.hanto.dragndrop.data.MainDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "main_database"
        ).build()
    }


    @Provides
    fun provideRetailDao(database: AppDatabase): MainDao {
        return database.MainDao()
    }

}