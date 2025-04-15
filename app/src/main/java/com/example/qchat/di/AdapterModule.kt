package com.example.qchat.di

import android.content.SharedPreferences
import com.example.qchat.adapter.GroupMessagesAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AdapterModule {

    @Provides
    @Singleton
    fun provideGroupMessagesAdapter(preferences: SharedPreferences): GroupMessagesAdapter {
        return GroupMessagesAdapter(preferences)
    }
} 