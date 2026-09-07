package com.personal.moneytracker.data.repository

import com.personal.moneytracker.data.local.dao.RawEventDao
import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

interface RawEventRepository {
    fun observeAll(): Flow<List<RawNotificationEvent>>
    suspend fun save(event: RawNotificationEvent): Boolean
}

class DefaultRawEventRepository @Inject constructor(private val dao: RawEventDao) : RawEventRepository {
    override fun observeAll(): Flow<List<RawNotificationEvent>> = dao.observeAll()
    override suspend fun save(event: RawNotificationEvent): Boolean = dao.insert(event) != -1L
}
