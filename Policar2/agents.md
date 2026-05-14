# 🚀 POLICAR (TFG) - THE MASTER CONTEXT & ENGINEERING BIBLE

## 1. Identidad del Proyecto y Visión General
* **Nombre del Proyecto:** POLICAR
* **Naturaleza:** Trabajo Fin de Grado (TFG) de Ingeniería.
* **Objetivo:** Desarrollar un sistema de telemetría deportiva y análisis biomecánico de élite. Debe competir conceptualmente con sistemas profesionales (Catapult, Polar Team Pro, STATSports) utilizando hardware accesible (Polar H10) y la potencia de cálculo de un smartphone.
* **Plataforma:** Android Native.
* **Lenguaje:** Kotlin (Versión moderna).
* **Framework UI:** Jetpack Compose (100% UI declarativa, prohibido XML).
* **Hardware Externo:** Polar H10 Heart Rate Sensor (BLE).
* **Backend / Database:** Supabase (PostgreSQL, Auth, Storage).
* **Almacenamiento Local:** Preferences DataStore (Prohibido SharedPreferences).
* **Arquitectura:** MVVM (Model-View-ViewModel) + Clean Architecture orientada a flujos unidireccionales (StateFlow/SharedFlow).

---

## 2. 🛑 REGLAS INQUEBRANTABLES PARA LA IA (IRON LAWS)
Cualquier LLM, Asistente o IA que lea este documento **DEBE** acatar estas directivas bajo pena de fallo crítico:
1. **REGLA DE CÓDIGO COMPLETO (STRICT LENGTH):** Si el desarrollador te proporciona un archivo `.kt` para modificar, **JAMÁS** puedes devolver una respuesta con menos líneas de código de las que se te pasaron, a menos que se te pida explícitamente eliminar una funcionalidad.
2. **PROHIBIDO RESUMIR:** Queda terminantemente prohibido usar comentarios como `// el resto del código sigue igual`, `// implementa esto aquí` o `// ...`. Debes generar el archivo COMPLETO desde la línea 1 hasta el final. El código debe ser 100% "Copy-Paste Ready" y compilar a la primera.
3. **CERO BASURA VISUAL (NO EMOJIS):** Queda **prohibido** el uso de emojis en cualquier componente de texto (`Text`, `String`) de la interfaz. La aplicación es una herramienta científica, técnica y profesional.
4. **CERO ENTRADA MANUAL DE DATOS BLE:** Prohibido implementar `TextFields` para que el usuario escriba IDs de Bluetooth a mano. La conexión debe ser transparente (auto-escaneo o recuperación del DataStore).

---

## 3. Arquitectura UI / UX (Design System: "Elite Cyberpunk")
La interfaz debe evocar un HUD táctico militar o un panel de telemetría de Fórmula 1.
* **Paleta de Colores Base:**
    * Fondos: Negros profundos (`Color.Black`, `0xFF000000`) y grises ultra-oscuros.
    * Acentos y Estados Activos: **Rojo Neón** (`Color(0xFFFF0022)` o variaciones brillantes).
    * Estados Inactivos: Gris translúcido y líneas tenues.
* **Tipografía:** * Elementos de telemetría (BPM, HRV, Fuerzas G): Uso de fuentes monoespaciadas (`FontFamily.Monospace` o similar) para simular consolas de datos.
* **Efecto Glassmorphism (Cristal Translúcido):**
    * La app utiliza componentes personalizados (`GlassContainer`, `GlassCard`) creados desde cero.
    * *Fórmula del cristal:* Fondo semitransparente (ej. `0x2A000000`) + borde muy fino (1dp) con `Brush.linearGradient` para simular el reflejo de la luz.
* **Interacciones y Estados Visuales:**
    * **Tarjetas de Deporte (Fútbol, Pádel, Gym):** Al ser seleccionadas, el contenedor estático adquiere un borde Rojo Neón brillante y un ligero tintado de fondo. PROHIBIDO usar animaciones de rotación (`Modifier.rotate`) en las tarjetas.
    * **Iconos:** Los iconos deben ser limpios, con fondo transparente (prohibido usar bloques sólidos rojos como fondo de un icono).
* **Navegación Restringida (FAB):** El botón de "EMPEZAR ENTRENAMIENTO" (ubicado en la parte inferior) **solo debe existir/habilitarse** si el ViewModel reporta: `isConnected == true` Y `selectedSport != null`. Prohibido añadir botones "fantasma" que no tengan función lógica.

---

## 4. Hardware y Polar BLE SDK (Core Técnico)
El manejo de la API de Polar es el núcleo del proyecto. Se exige un manejo exhaustivo de permisos, estados y callbacks.
* **Permisos Obligatorios en AndroidManifest:** `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`.
* **Requisito de Firmware:** La banda debe tener la versión `3.0.35` o superior para permitir lectura de ECG y ACC.
* **Auto-Reconexión (Zero Friction):**
    * Tras una conexión exitosa, el `deviceId` se guarda en **Preferences DataStore**.
    * Al iniciar la app, el `PolarManager` debe intentar conectarse silenciosamente al último dispositivo conocido.
* **Resiliencia y Pérdida de Señal (Modo Vivo):**
    * Alcance BLE real: ~10-15 metros.
    * Si el atleta se aleja, la app **NO debe crashear** ni terminar la sesión. El estado en Compose debe cambiar a `"Buscando reconexión..."` y el SDK debe reanudar el flujo en cuanto el jugador entre en rango.

---

## 5. Modos de Operación y Telemetría

### A. Modo "Live Telemetry" (Telemetría en Vivo)
* **Casos de Uso:** Entrenamientos de gimnasio, porteros de fútbol (móvil junto al poste), o atletas con chaleco GPS/móvil incorporado.
* **Métricas Extraídas en Directo:** BPM (Heart Rate), HRV (Variabilidad Cardíaca), ECG (Electrocardiograma puro), Acelerómetro 3D (Ejes X, Y, Z en m/s² o Fuerzas G).
* **Algoritmos Biomecánicos a Implementar (El "Motor Pro-Max"):**
    1. *Frenadas Bruscas:* Detección de picos negativos masivos de aceleración (deceleración en seco) en el eje de avance tras un esfuerzo sostenido.
    2. *Duelos Aéreos (Tiempo de Vuelo):* Patrón específico en el eje Z (Vertical): `Pico de Impulso -> Fase G-Zero (Vuelo) -> Pico de Impacto (Aterrizaje)`.
    3. *High-Intensity Efforts (HIE):* Lógica combinada: `SI (Aceleración > Límite_G) Y (Cardio > Zona_Roja_BPM) ENTONCES registrar_HIE()`.

### B. Modo "Memoria Interna" (Offline Recording / Nivel DIOS)
* **Casos de Uso:** Partidos de Fútbol o Pádel reales donde es imposible llevar el móvil encima.
* **Limitación Física del Hardware:** En este modo, el Polar H10 **SOLO** graba Frecuencia Cardíaca e Intervalos (RR). No guarda ECG ni ACC debido a limitaciones de la memoria flash de la pastilla.
* **Pipeline de Sincronización Estricto (Obligatorio seguir este orden):**
    1. **Inicio:** Llamada a `api.startRecording()`. La UI muestra "Grabando en sensor". El usuario puede cerrar la app y alejarse.
    2. **Fin:** El usuario vuelve al rango del móvil y pulsa "Detener y Descargar". Se llama a `api.stopRecording()`.
    3. **Descarga:** Uso de `api.listExercises()` para obtener la referencia, y `api.fetchExercise()` para volcar los datos binarios al móvil.
    4. **Subida (Cloud):** Se mapean los datos descargados y se insertan en la tabla correspondiente de **Supabase**.
    5. **Limpieza Automática (CRÍTICO):** ÚNICA Y EXCLUSIVAMENTE cuando Supabase confirme la subida (Success 200), se debe ejecutar `api.removeExercise()` para borrar la memoria de la banda y evitar que se bloquee en el futuro.

---

## 6. Integración Backend: Supabase
* **Base de Datos:** PostgreSQL.
* **Esquema de Datos (Asumido para generación de código):**
    * Tabla `sessions`: Guarda los metadatos del entrenamiento (ID, User_ID, Deporte, Fecha, Duración, Modo Vivo/Offline).
    * Tabla `telemetry_data`: Guarda los flujos pesados (BPM por segundo, impactos, fuerzas G) asociados a una `session_id`.
* **Corrutinas:** Todas las llamadas a Supabase deben ejecutarse en `Dispatchers.IO` y manejarse con bloques `try-catch` robustos para evitar bloqueos en el Hilo Principal (Main Thread) de la UI.

---

## 7. Manejo de Estados Unidireccional (UDF)
La arquitectura prohíbe pasar el `PolarManager` directamente a las vistas de Compose.
* **El ViewModel es el Rey:** Todas las interacciones de Compose envían "Intents/Events" al ViewModel.
* **El ViewModel gestiona el PolarManager:** Escucha los callbacks del SDK y actualiza un `StateFlow<HomeState>` o `StateFlow<WorkoutState>`.
* **Estados típicos esperados:** `Idle`, `Scanning`, `Connecting`, `LiveStreaming`, `OfflineRecording`, `Downloading`, `SyncingCloud`, `Error(message)`. Compose solo reacciona (recompone) basándose en estos estados.