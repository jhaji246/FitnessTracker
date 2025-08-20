package com.avi.auth.domain

interface PatternValidator {
    fun matches(value: String): Boolean
}