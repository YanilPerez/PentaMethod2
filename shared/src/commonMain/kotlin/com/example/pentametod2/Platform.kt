package com.example.pentametod2

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform