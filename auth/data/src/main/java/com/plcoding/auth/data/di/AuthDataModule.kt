package com.avi.auth.data.di

import com.avi.auth.data.AuthRepositoryImpl
import com.avi.auth.data.EmailPatternValidator
import com.avi.auth.domain.AuthRepository
import com.avi.auth.domain.PatternValidator
import com.avi.auth.domain.UserDataValidator
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

val authDataModule = module {
    single<PatternValidator> {
        EmailPatternValidator
    }
    singleOf(::UserDataValidator)
    singleOf(::AuthRepositoryImpl).bind<AuthRepository>()
}