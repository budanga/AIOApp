package com.example.aioapp.core.model

sealed class AppLanguage(val tag: String) {
    data object System : AppLanguage("system")
    data object English : AppLanguage("en")
    data object Spanish : AppLanguage("es")

    companion object {
        fun fromTag(tag: String): AppLanguage = when (tag) {
            "en" -> English
            "es" -> Spanish
            else -> System
        }
    }
}
