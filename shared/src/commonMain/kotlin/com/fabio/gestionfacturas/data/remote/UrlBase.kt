package com.fabio.gestionfacturas.data.remote

/**
 * Backend base URL, which differs per platform (Desktop: localhost; Android emulator: 10.0.2.2).
 * A physical phone would need the PC's LAN IP: change the Android actual when testing on one.
 */
expect fun urlBaseApi(): String
