# GestionFacturas — Cliente (Android + Desktop)

> **Proyecto en fase inicial.** El proyecto base está generado pero aún no hay
> funcionalidad implementada. Este README describe el diseño acordado y evoluciona con el
> código.

Cliente de **GestionFacturas**, una aplicación para que **freelances y autónomos** lleven
el control de su actividad económica: registran **gastos**, **ingresos** y **clientes**, y
consultan un panel con su beneficio y la rentabilidad por cliente.

Es una app de **Kotlin Multiplatform con Compose Multiplatform**: una sola interfaz
compartida para **Android y escritorio**. Se desarrolla primero la versión de escritorio y
después se adapta el diseño a móvil.

Principio rector: **organización y visibilidad, nunca asesoría fiscal ni facturación
oficial.**

## Cómo encaja en el proyecto

Este repositorio es **solo el cliente**. Toda la lógica de negocio (beneficio, totales,
transiciones de cobro, control de acceso) vive en el backend:

- **Backend (Spring Boot, arquitectura hexagonal):**
  [GestionFacturas-api](https://github.com/hagi33/GestionFacturas-api)
- **Cliente Android + Desktop (este repo):** consume la API REST con autenticación JWT.
- **Cliente web:** proyecto aparte con Angular, sobre la misma API *(planificado)*.

iOS queda fuera del alcance por no disponer de Mac para compilarlo.

## Stack

| Capa | Tecnología |
|------|-----------|
| Lenguaje y UI | Kotlin Multiplatform + Compose Multiplatform (Material 3) |
| Red | Ktor Client + kotlinx.serialization |
| Asincronía | kotlinx.coroutines |
| Inyección de dependencias | Koin |
| Almacenamiento local | Multiplatform Settings (solo el token) |
| Navegación | Navigation Compose (oficial, multiplataforma) |
| Logging | Kermit |
| Build | Gradle (Kotlin DSL) |

## Módulos de Gradle

| Módulo | Contenido |
|--------|-----------|
| `:shared` | Todo el código compartido (datos, dominio, UI). El escritorio es el target `jvmMain` |
| `:androidApp` | Punto de entrada de Android (actividad, manifiesto) |
| `:desktopApp` | Punto de entrada de escritorio (función `main`, empaquetado) |

Las dos apps son finas y solo llaman a `:shared`.

## Arquitectura

**MVVM + Clean Architecture ligera.** Las dependencias apuntan hacia el dominio:

```mermaid
flowchart LR
    UI["ui<br/>pantallas + ViewModels"] --> DOM["domain<br/>modelos + interfaces de repositorio"]
    DATA["data<br/>repositorios, API (Ktor), token"] --> DOM
```

- **ui:** pantallas en Compose y ViewModels. Cada ViewModel expone un único `UiState` y la
  pantalla solo lo pinta. Depende únicamente de `domain`.
- **domain:** Kotlin puro. Modelos e interfaces de repositorio. Los casos de uso solo
  existen cuando una operación orquesta varias fuentes de datos (por ejemplo, cerrar
  sesión: revocar el token en el backend y borrar el local).
- **data:** implementaciones de los repositorios, llamadas a la API, DTOs y
  almacenamiento del token.

Se mantienen separados el DTO de red, el modelo de dominio y el `UiState`, con los mapeos
en la capa de datos. Es la misma idea de inversión de dependencias que usa el backend, pero
con menos ceremonia, porque el cliente no contiene lógica de negocio.

### Estructura de paquetes

Paquete raíz `com.fabio.gestionfacturas`, dentro de `shared/src/commonMain/kotlin/`:

```
├── data/      remote/ (Ktor, DTOs)  ·  local/ (token)  ·  repository/
├── domain/    model/  ·  repository/ (interfaces)  ·  usecase/ (solo si orquesta varias fuentes)
├── ui/        navigation/  ·  login/  clientes/  gastos/  ingresos/  dashboard/  ·  common/
└── di/        módulos de Koin
```

El código específico de plataforma (expect/actual) va en `shared/src/androidMain` y
`shared/src/jvmMain`.

## Decisiones de diseño

| Decisión | Elección | Motivo |
|----------|----------|--------|
| Plataformas | Android + Desktop (web aparte) | Sin Mac no hay iOS; la web con Angular reutiliza una habilidad ya probada |
| Patrón | MVVM con `UiState` único | Estándar, sin librerías extra, fácil de testear |
| Capas | Clean ligera | El cliente no tiene reglas de negocio: los casos de uso serían clases vacías |
| Carpetas | Capas arriba, áreas dentro de `ui/` | Simple, sin repartir cada pantalla en tres sitios |
| Navegación | Navigation Compose oficial | Soporte oficial, se integra con ViewModel y Koin |
| Almacenamiento | Solo el token, sin caché offline | La app es online; se amplía solo si surge la necesidad |

**Pendiente de decidir:** cómo representar los importes (`BigDecimal` no existe en código
común de Kotlin Multiplatform). Se resolverá al construir la parte de gastos.

## Estado actual

- [x] Proyecto base generado (Android + Desktop)
- [ ] Login y registro
- [ ] Clientes
- [ ] Gastos
- [ ] Ingresos y cobros
- [ ] Dashboard (resumen, rentabilidad por cliente, pendientes de cobro)
- [ ] Adaptación del diseño a móvil

Se construye por **rebanadas verticales**: cada área se completa de punta a punta (red,
datos, pantalla) antes de empezar la siguiente.

## Cómo arrancar (desarrollo)

**Requisitos:** Android Studio (o IntelliJ IDEA) con el plugin de Kotlin Multiplatform, y el
**backend en ejecución** (ver su README: PostgreSQL con Docker y un usuario registrado).

```bash
# Escritorio (el ciclo de desarrollo más rápido)
./gradlew :desktopApp:run

# Tests del código compartido
./gradlew :shared:jvmTest
```

Para Android, ejecuta la configuración de `androidApp` desde el IDE, con un emulador o un
móvil con depuración USB.

**URL del backend según plataforma:**

| Plataforma | URL |
|------------|-----|
| Desktop | `http://localhost:8080` |
| Emulador de Android | `http://10.0.2.2:8080` |
| Móvil físico | IP del PC en la red local |

En Android el tráfico HTTP sin cifrar está bloqueado por defecto; en desarrollo se permite
solo en la compilación de depuración.

## Notas de seguridad

- El token se guarda con Multiplatform Settings, que **no cifra**. Es aceptable para este
  proyecto personal; el almacenamiento seguro (por ejemplo, Android Keystore) queda como
  mejora pendiente.
- El backend limita las peticiones (login: 10/min por IP; resto: 50/min por usuario). El
  cliente debe tratar el HTTP 429 con un mensaje claro.

---

*Proyecto personal en desarrollo. La documentación se amplía conforme avanzan las fases.*
