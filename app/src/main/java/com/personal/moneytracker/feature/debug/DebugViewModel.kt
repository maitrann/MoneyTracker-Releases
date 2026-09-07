package com.personal.moneytracker.feature.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.moneytracker.data.local.entity.ParsedObservation
import com.personal.moneytracker.data.local.entity.RawNotificationEvent
import com.personal.moneytracker.data.repository.ObservationRepository
import com.personal.moneytracker.data.repository.RawEventRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class DebugEvent(val raw: RawNotificationEvent, val observation: ParsedObservation?)

@HiltViewModel
class DebugViewModel @Inject constructor(
    rawEvents: RawEventRepository,
    observations: ObservationRepository,
) : ViewModel() {
    val events = combine(rawEvents.observeAll(), observations.observeAll()) { raw, parsed ->
        val byRawId = parsed.associateBy { it.rawEventId }
        raw.map { DebugEvent(it, byRawId[it.id]) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
