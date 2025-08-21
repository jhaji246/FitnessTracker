package com.avi.auth.presentation.intro

sealed interface IntroAction {
    data object OnSignInClick: IntroAction
    data object OnSignUpClick: IntroAction
    data object OnSkip: IntroAction
    data object OnGetStarted: IntroAction
}