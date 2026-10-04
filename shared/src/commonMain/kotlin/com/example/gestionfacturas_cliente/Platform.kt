package com.example.gestionfacturas_cliente

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform