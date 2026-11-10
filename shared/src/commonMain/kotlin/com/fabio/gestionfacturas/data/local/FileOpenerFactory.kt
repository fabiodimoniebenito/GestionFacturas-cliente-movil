package com.fabio.gestionfacturas.data.local

import com.fabio.gestionfacturas.domain.repository.FileOpener

/** The platform's [FileOpener]. Actuals live in jvmMain (native open dialog) and androidMain (stub for now). */
expect fun crearFileOpener(): FileOpener
