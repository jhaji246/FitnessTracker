package com.avi.analytics.presentation

sealed interface AnalyticsAction {
    data object OnBackClick: AnalyticsAction
}