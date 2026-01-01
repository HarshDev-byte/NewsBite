package com.example.newsbite.util

/**
 * A generic class that holds a value with its loading status.
 */
sealed class Resource<out T> {
    data class Success<out T>(val data: T) : Resource<T>()
    data class Error(val message: String) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
    
    val status: Status
        get() = when (this) {
            is Success -> Status.SUCCESS
            is Error -> Status.ERROR
            is Loading -> Status.LOADING
        }
    
    enum class Status {
        SUCCESS,
        ERROR,
        LOADING
    }
}
