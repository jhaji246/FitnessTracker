package com.avi.wear.run.presentation

import com.avi.core.presentation.ui.UiText

sealed interface TrackerEvent {
    data object RunFinished: TrackerEvent
    data class Error(val message: UiText): TrackerEvent
}