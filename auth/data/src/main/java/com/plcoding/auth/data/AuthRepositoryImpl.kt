package com.avi.auth.data

import com.avi.auth.domain.AuthRepository
import com.avi.core.data.networking.post
import com.avi.core.domain.AuthInfo
import com.avi.core.domain.SessionStorage
import com.avi.core.domain.util.DataError
import com.avi.core.domain.util.EmptyResult
import com.avi.core.domain.util.Result
import com.avi.core.domain.util.asEmptyDataResult
import io.ktor.client.HttpClient
import kotlinx.coroutines.delay

class AuthRepositoryImpl(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage
): AuthRepository {

    override suspend fun login(email: String, password: String): EmptyResult<DataError.Network> {
        // For development, use mock login
        delay(1000) // Simulate network delay
        
        // Simple mock authentication
        if (email.isNotEmpty() && password.isNotEmpty()) {
            val mockAuthInfo = AuthInfo(
                accessToken = "mock_access_token_${System.currentTimeMillis()}",
                refreshToken = "mock_refresh_token_${System.currentTimeMillis()}",
                userId = "mock_user_${System.currentTimeMillis()}"
            )
            sessionStorage.set(mockAuthInfo)
            return Result.Success(Unit).asEmptyDataResult()
        } else {
            return Result.Error(DataError.Network.UNAUTHORIZED).asEmptyDataResult()
        }
    }

    override suspend fun register(email: String, password: String): EmptyResult<DataError.Network> {
        // For development, use mock registration
        delay(1000) // Simulate network delay
        
        // Simple mock registration - always succeed for development
        return Result.Success(Unit).asEmptyDataResult()
    }
}