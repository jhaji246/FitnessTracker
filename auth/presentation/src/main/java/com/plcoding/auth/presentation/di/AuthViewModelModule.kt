package com.avi.auth.presentation.di

import com.avi.auth.presentation.login.LoginViewModel
import com.avi.auth.presentation.register.RegisterViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authViewModelModule = module {
    viewModelOf(::RegisterViewModel)
    viewModelOf(::LoginViewModel)
}