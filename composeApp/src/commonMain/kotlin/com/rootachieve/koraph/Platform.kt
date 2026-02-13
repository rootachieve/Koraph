package com.rootachieve.koraph

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform