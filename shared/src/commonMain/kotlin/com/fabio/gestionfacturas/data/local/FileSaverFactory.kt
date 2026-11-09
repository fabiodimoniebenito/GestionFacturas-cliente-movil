package com.fabio.gestionfacturas.data.local

import com.fabio.gestionfacturas.domain.repository.FileSaver

/** The platform's [FileSaver]. Actuals live in jvmMain (native save dialog) and androidMain. */
expect fun crearFileSaver(): FileSaver
