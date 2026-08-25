# Plan: BuscadorAndroid - Buscador de Archivos para Todo el Celular

> Estado: **Propuesta** | Fecha: 2026-08-24 | Autor: Muse Spark

## 1. Visión y Objetivo

Construir una app nativa Android que permita buscar cualquier archivo en todo el dispositivo de forma rápida, precisa y con permisos mínimos necesarios, funcionando desde Android 8 (API 26) hasta Android 15 (API 35).

**Problema a resolver:** El gestor de archivos nativo de Android no tiene búsqueda potente, no indexa bien, no filtra por contenido/tipo/fecha y es lento en dispositivos con miles de archivos.

**Usuario objetivo:** Usuario general que necesita encontrar fotos, PDFs, APKs, videos, descargas perdidas sin saber en qué carpeta están.

---

## 2. Alcance Funcional (MVP + Futuro)

### 2.1 MVP - Fase 1 (Imprescindible)
- [ ] Búsqueda por nombre (coincidencia parcial, insensible a mayúsculas, con y sin acentos)
- [ ] Filtros básicos: tipo de archivo (imagen, video, audio, documento, apk, comprimido), extensión
- [ ] Filtros por tamaño y fecha de modificación
- [ ] Resultados en tiempo real mientras se escribe (debounce 300ms)
- [ ] Vista de resultados con nombre, ruta, tamaño, fecha, miniatura para imágenes/video
- [ ] Acciones: abrir, compartir, eliminar, copiar ruta, ver propiedades, abrir carpeta contenedora
- [ ] Ordenamiento: por relevancia, nombre, fecha, tamaño, tipo
- [ ] Permisos: solicitar `READ_MEDIA_*` (Android 13+) y `READ_EXTERNAL_STORAGE` (compat), y `MANAGE_EXTERNAL_STORAGE` opcional para búsqueda total

### 2.2 Fase 2 - Búsqueda Avanzada
- [ ] Búsqueda por contenido de texto (dentro de .txt, .csv, .log, .md)
- [ ] Expresiones regulares y comodines (`*.pdf`, `foto_202*`)
- [ ] Búsqueda por rango de fechas y tamaño con sliders
- [ ] Búsqueda duplicados por hash (MD5/SHA-1)
- [ ] Historial de búsquedas y búsquedas guardadas/favoritas
- [ ] Indexado en segundo plano con WorkManager + Room para búsquedas instantáneas

### 2.3 Fase 3 - Pulido y Potencia
- [ ] Búsqueda en tarjeta SD y USB OTG (Storage Access Framework)
- [ ] Filtros por ubicación (solo Descargas, solo DCIM, solo WhatsApp, ruta personalizada)
- [ ] Preview integrado (visor de texto, imagen, PDF)
- [ ] Estadísticas de almacenamiento (qué ocupa más, archivos grandes olvidados)
- [ ] Modo oscuro, Material You (Material 3), soporte multi-idioma

**Fuera de alcance inicial:** Búsqueda en la nube (Drive, Dropbox), búsqueda dentro de APK/ZIP sin extraer, acceso root.

---

## 3. Arquitectura Técnica

### 3.1 Stack Recomendado
```
Lenguaje: Kotlin 100%
UI: Jetpack Compose + Material 3
Arquitectura: MVVM + Clean Architecture (data / domain / presentation)
DI: Hilt (o Koin si se quiere más ligero)
Asíncrono: Coroutines + Flow
Navegación: Navigation Compose
Persistencia: Room (índice y historial) + DataStore (preferencias)
Búsqueda: MediaStore API + File API (con MANAGE_ALL_FILES) + SAF
Background: WorkManager para indexación
Permisos: Accompanist Permissions + Activity Result API
Test: JUnit4, Turbine, Compose Test, Espresso
Mín SDK: 26 (Android 8) | Target SDK: 35 (Android 15)
```

### 3.2 Estructura de Módulos
```
app/
 ├── data/
 │    ├── local/ (Room: entidades ArchivoIndexado, HistorialBusqueda)
 │    ├── mediastore/ (MediaStoreDataSource - consulta a ContentResolver)
 │    ├── filesystem/ (FileSystemDataSource - java.io.File con permisos totales)
 │    └── repository/ (BusquedaRepositoryImpl)
 ├── domain/
 │    ├── model/ (Archivo, TipoArchivo, FiltroBusqueda, ResultadoBusqueda)
 │    ├── repository/ (BusquedaRepository interface)
 │    └── usecase/ (BuscarArchivosUseCase, ObtenerFiltrosUseCase)
 ├── presentation/
 │    ├── ui/ (PantallaBusqueda, PantallaFiltros, PantallaDetalles)
 │    ├── viewmodel/ (BusquedaViewModel con StateFlow)
 │    └── component/ (BarraBusqueda, ChipFiltro, ItemArchivo)
 └── worker/ (IndexacionWorker)
```

### 3.3 Modelo de Datos Principal
```kotlin
data class Archivo(
    val id: Long,
    val nombre: String,
    val nombreNormalizado: String, // sin acentos, lowerCase para búsqueda
    val ruta: String,
    val uri: Uri,
    val tamanoBytes: Long,
    val fechaModificacion: Long,
    val mimeType: String?,
    val tipo: TipoArchivo, // IMAGEN, VIDEO, AUDIO, DOCUMENTO, APK, COMPRIMIDO, OTRO
    val extension: String
)

data class FiltroBusqueda(
    val query: String = "",
    val tipos: Set<TipoArchivo> = emptySet(),
    val extensiones: Set<String> = emptySet(),
    val tamanoMin: Long? = null,
    val tamanoMax: Long? = null,
    val fechaDesde: Long? = null,
    val fechaHasta: Long? = null,
    val soloEnRuta: String? = null,
    val usarRegex: Boolean = false
)
```

---

## 4. Estrategia de Búsqueda - El Núcleo

Este es el punto más crítico por las restricciones de Android moderno (Scoped Storage desde Android 10).

### 4.1 Enfoque Híbrido (Recomendado)
1.  **MediaStore (principal):** Consultar `MediaStore.Files`, `MediaStore.Images`, `MediaStore.Video`, `MediaStore.Audio`, `MediaStore.Downloads` vía `ContentResolver`. Es rápido, no requiere `MANAGE_EXTERNAL_STORAGE` para media, y funciona en Android 10+.
    - Pros: Rápido, compatible con permisos granulares de Android 13+ (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`).
    - Contras: No indexa todos los archivos (ej. archivos ocultos, carpetas de apps).

2.  **File API directo (complemento):** Si el usuario concede `MANAGE_EXTERNAL_STORAGE` (`ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION`), recorrer recursivamente con `java.io.File` o `DocumentFile` desde `Environment.getExternalStorageDirectory()`. Esto da acceso TOTAL.
    - Se debe justificar en Play Store (declarar que es app de gestión de archivos/búsqueda).

3.  **Storage Access Framework (SAF) (fallback):** Para tarjeta SD y para usuarios que no quieren dar acceso total, usar `Intent.ACTION_OPEN_DOCUMENT_TREE` para que elija carpetas.

**Decisión:** Implementar 1 + 2. Solicitar permisos progresivamente: primero media, luego opción "Búsqueda profunda (acceso total)" que explica por qué se necesita y lleva a ajustes.

### 4.2 Algoritmo de Búsqueda
- Normalizar query y nombres: `lowercase + sin acentos (Normalizer) + trim`
- Coincidencia: `contains` por defecto. Opcional `regex` y `glob -> regex`
- Debounce en ViewModel: `queryFlow.debounce(300ms).distinctUntilChanged().flatMapLatest { repo.buscar(it) }`
- Ranking: coincidencias al inicio del nombre pesan más, luego por fecha reciente
- Paginación: `Paging 3` para no cargar 100k resultados de golpe

### 4.3 Indexación para Velocidad
- Tabla Room `archivos_indexados` con FTS4 (`@Fts4`) para búsqueda full-text instantánea
- `IndexacionWorker` periódico (cada 6h o al detectar `ContentObserver` en MediaStore) que sincroniza MediaStore -> Room
- Búsqueda primero en Room (instantánea), si no hay índice, fallback a MediaStore en vivo

---

## 5. Permisos y Compatibilidad

| Permiso | Cuándo | Android |
|---|---|---|
| `READ_EXTERNAL_STORAGE` | Lectura general (API < 33) | 26-32 |
| `READ_MEDIA_IMAGES/VIDEO/AUDIO` | Media granular | 33+ |
| `MANAGE_EXTERNAL_STORAGE` | Búsqueda total en todo el sistema | 30+ (opcional) |
| `QUERY_ALL_PACKAGES` | NO necesario | - |
| `ACCESS_MEDIA_LOCATION` | Opcional para fotos | 29+ |

Flujo de permisos en UI:
1. Pantalla onboarding explicando por qué se necesita cada permiso
2. Solicitud secuencial con `rememberLauncherForActivityResult`
3. Si deniega, mostrar estado vacío con botón "Conceder permisos"
4. Para `MANAGE_EXTERNAL_STORAGE`, botón que abre `Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION` con diálogo explicativo previo (requisito Play Store)

---

## 6. Diseño de UI/UX (Material 3)

**Pantalla Principal (Búsqueda):**
```
[ Barra superior Material 3 ]
[ Campo búsqueda con icono lupa + botón voz + botón filtros ]
[ Chips rápidos: Imágenes | Videos | Documentos | APKs | Grandes ]
[ Lista resultados (LazyColumn con miniatura + nombre + ruta tenue + tamaño) ]
[ FAB: Escanear/indexar ahora ]
[ Bottom bar: Historial | Favoritos | Ajustes ]
```

**Estados:**
- Vacío inicial: ilustración + "Busca cualquier archivo"
- Sin permiso: tarjeta explicativa + botón conceder
- Cargando: shimmer
- Sin resultados: "No se encontró 'query' - prueba con filtros"
- Con resultados: contador "1,234 archivos encontrados en 0.42s"

**Detalles de archivo (BottomSheet):** preview, ruta completa copiable, botón abrir con app, compartir, eliminar con confirmación, propiedades.

---

## 7. Plan de Implementación por Fases

### Fase 0 - Preparación (1-2 días) ✅ COMPLETADA
- [x] Crear proyecto Android Studio (Empty Compose Activity, Kotlin, minSdk 26, targetSdk 35)
- [x] Configurar Hilt, Room, Navigation, Permisos
- [x] Crear estructura de paquetes (data/domain/presentation)
- [x] Configurar `AndroidManifest.xml` con permisos
- [x] Modelo `TipoArchivo` con 6 categorías + extensiones comunes (imagen/música/video/documento/apk/comprimido)
- [x] `FiltroBusqueda` con unión de tipos + extensiones manuales (`extensionesEfectivas`)
- [x] `MediaStoreDataSource` + `FileSystemDataSource` con filtro por tipo/extensión
- [x] UI con `FilaChipsTipo` y diálogo `+ Extensión`

### Fase 1 - MVP Búsqueda Básica (1-2 semanas)
- [ ] Implementar `MediaStoreDataSource` - query a `MediaStore.Files.getContentUri("external")`
- [ ] Crear `BusquedaRepository` + `BuscarArchivosUseCase`
- [ ] `BusquedaViewModel` con `StateFlow<FiltroBusqueda>` y `Flow<PagingData<Archivo>>`
- [ ] UI: barra de búsqueda + lista resultados + filtros básicos por tipo
- [ ] Gestión de permisos completa
- [ ] Acciones sobre archivo (abrir via Intent, compartir, info)

### Fase 2 - Filtros y Rendimiento (1 semana)
- [ ] Filtros avanzados (tamaño, fecha, extensión)
- [ ] Ordenamiento
- [ ] Room + FTS + IndexacionWorker
- [ ] `FileSystemDataSource` para modo acceso total

### Fase 3 - Pulido y Publicación (1 semana)
- [ ] Soporte SD/SAF
- [ ] Historial y favoritos
- [ ] Tests instrumentados y unitarios
- [ ] Icono, splash, onboarding, política de privacidad (requerida por MANAGE_EXTERNAL_STORAGE)
- [ ] Preparar ficha Play Store y AAB firmado
- [ ] Pruebas en Android 8, 11, 13, 14, 15 (emuladores)

**Estimación total MVP publicable: 3-4 semanas (1 dev)**

---

## 8. Riesgos y Mitigaciones

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Rechazo en Play Store por `MANAGE_EXTERNAL_STORAGE` | Alto | Hacerlo opcional, justificar como "file manager/search tool", grabar video demostrativo, tener política de privacidad |
| Scoped Storage limita visibilidad | Alto | Estrategia híbrida MediaStore + File + SAF, explicar al usuario |
| Rendimiento con 100k+ archivos | Medio | Paginación, Room FTS, búsqueda en Dispatchers.IO, evitar `File.listFiles()` recursivo sin límites |
| Permisos confusos para usuario | Medio | Onboarding claro, solicitud progresiva, mensajes en español |
| Archivos ocultos / carpetas de apps privadas | Bajo | Documentar limitación (sin root no se puede acceder a `/data/data`) |

---

## 9. Criterios de Éxito

- Búsqueda de "vacaciones" en dispositivo con 20k archivos devuelve resultados en < 1 segundo (con índice) o < 3 segundos (sin índice)
- Funciona en Android 8 a 15 sin crash de permisos
- Tasa de permisos concedidos > 80% gracias a onboarding
- < 15 MB tamaño APK/AAB

---

## 10. Próximos Pasos Inmediatos

1. **Aprobar este plan** - confirmar stack (¿Hilt vs Koin? ¿minSdk 26 ok?)
2. Ejecutar Fase 0: `Crear proyecto` (puedo generarlo ahora si confirmas)
3. Definir nombre final y paquete (`com.buscadorandroid.app` sugerido)
4. Decidir si `MANAGE_EXTERNAL_STORAGE` será opcional u obligatorio desde el inicio

---

## 11. Reglas del Proyecto

Ver `AGENTS.md` - Comunicación y razonamiento exclusivamente en español.

## Referencias Técnicas
- MediaStore: https://developer.android.com/training/data-storage/shared/media
- Manage all files: https://developer.android.com/privacy-and-security/storage
- SAF: https://developer.android.com/guide/topics/providers/document-provider
- Room FTS: https://developer.android.com/training/data-storage/room/defining-data
