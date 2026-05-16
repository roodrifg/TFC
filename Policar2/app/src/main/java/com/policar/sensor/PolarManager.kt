package com.policar.sensor

import android.content.Context
import android.util.Log
import com.polar.sdk.api.PolarH10OfflineExerciseApi.RecordingInterval
import com.polar.sdk.api.PolarH10OfflineExerciseApi.SampleType
import com.policar.data.model.ConnectionState
import com.policar.data.model.DatosBiomecanicos
import com.policar.data.model.ExerciseRecordingData
import com.policar.data.model.FutbolBiomechanics
import com.policar.data.model.GymBiomechanics
import com.policar.data.model.GymRepData
import com.policar.data.model.PadelBiomechanics
import com.policar.data.model.RecordingStatus
import com.policar.data.model.SyncState
import com.policar.data.model.TipoDeporte
import com.polar.sdk.api.PolarBleApi
import com.polar.sdk.api.PolarBleApiCallback
import com.polar.sdk.api.PolarBleApiDefaultImpl
import com.polar.sdk.api.PolarBleApi.PolarBleSdkFeature
import com.polar.sdk.api.errors.PolarInvalidArgument
import com.polar.sdk.api.model.PolarDeviceInfo
import com.polar.sdk.api.model.PolarExerciseEntry
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.supervisorScope
import com.polar.sdk.api.model.PolarOfflineRecordingData
import com.polar.sdk.api.model.PolarOfflineRecordingEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.sqrt

private const val TAG = "PolarManager"

object PolarManagerProvider {
    @Volatile private var instance: PolarManager? = null
    fun get(context: Context): PolarManager =
        instance ?: synchronized(this) {
            instance ?: PolarManager(context.applicationContext).also { instance = it }
        }
}

class PolarManager(private val context: Context) {

    private val api: PolarBleApi = PolarBleApiDefaultImpl.defaultImplementation(
        context,
        setOf(
            PolarBleApi.PolarBleSdkFeature.FEATURE_HR,
            PolarBleApi.PolarBleSdkFeature.FEATURE_DEVICE_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_BATTERY_INFO,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_ONLINE_STREAMING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_RECORDING,
            PolarBleSdkFeature.FEATURE_POLAR_H10_EXERCISE_RECORDING,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_OFFLINE_EXERCISE_V2,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_DEVICE_TIME_SETUP,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_SDK_MODE,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_FILE_TRANSFER,
            PolarBleApi.PolarBleSdkFeature.FEATURE_POLAR_LED_ANIMATION
        )
    )

    private val bleExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "BLE coroutine exception (Polar SDK): ${throwable.javaClass.simpleName} — ${throwable.message}")
    }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob() + bleExceptionHandler)
    private val disposables = CompositeDisposable()

    private var _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private var _hrValue = MutableStateFlow(0)
    private var _hrvStress = MutableStateFlow(0.0)
    private var _rmssd = MutableStateFlow(0.0)
    private var _rrIntervalMs = MutableStateFlow(0L)
    private var _ecgSamples = MutableStateFlow<List<Float>>(emptyList())
    private var _biomechanics = MutableStateFlow(DatosBiomecanicos(TipoDeporte.FUTBOL.displayName))
    private var _futbolBio = MutableStateFlow(FutbolBiomechanics())
    private var _padelBio = MutableStateFlow(PadelBiomechanics())
    private var _gymBio = MutableStateFlow(GymBiomechanics())
    private var _hrHistory = MutableStateFlow<List<Int>>(emptyList())
    private var _recordingStatus = MutableStateFlow(RecordingStatus())
    private var _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    private var _discoveredDevices = MutableStateFlow<List<PolarDeviceInfo>>(emptyList())
    private var _isSearching = MutableStateFlow(false)

    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    val hrValue: StateFlow<Int> = _hrValue.asStateFlow()
    val hrvStress: StateFlow<Double> = _hrvStress.asStateFlow()
    val rmssd: StateFlow<Double> = _rmssd.asStateFlow()
    val rrIntervalMs: StateFlow<Long> = _rrIntervalMs.asStateFlow()
    val ecgSamples: StateFlow<List<Float>> = _ecgSamples.asStateFlow()
    val biomechanics: StateFlow<DatosBiomecanicos> = _biomechanics.asStateFlow()
    val futbolBio: StateFlow<FutbolBiomechanics> = _futbolBio.asStateFlow()
    val padelBio: StateFlow<PadelBiomechanics> = _padelBio.asStateFlow()
    val gymBio: StateFlow<GymBiomechanics> = _gymBio.asStateFlow()
    val hrHistory: StateFlow<List<Int>> = _hrHistory.asStateFlow()
    val recordingStatus: StateFlow<RecordingStatus> = _recordingStatus.asStateFlow()
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()
    val discoveredDevices: StateFlow<List<PolarDeviceInfo>> = _discoveredDevices.asStateFlow()
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val rrIntervals = mutableListOf<Int>()
    private val hrHistoryList = mutableListOf<Int>()
    private var repCount = 0
    private var setCount = 0
    private var lastRepTime = 0L
    private var firstRepVelocity = 0.0
    private var currentMechLoad = 0.0
    private var isConcentricPhase = false
    private var concentricStart = 0L
    private var smashCount = 0
    private var lastSmashTime = 0L
    private var lastImpactTime = 0L

    // Sprint detection (rolling 1-second window at 200Hz = 200 samples)
    private val sprintBuffer = ArrayDeque<Double>()
    private var sprintState = false
    private var sprintCount = 0

    // Asymmetry: rolling X-axis average (5s window = 1000 samples at 200Hz)
    private val xBuffer = ArrayDeque<Double>()
    private var xWindowSum = 0.0

    // Jump counting (reuses isInFlight/flightStartTime)
    private var jumpCount = 0
    private val gPeaksHistory = mutableListOf<Double>()
    private val gymRepsList = mutableListOf<GymRepData>()
    private var firstRepVelForLoss = 0f
    private var currentExerciseId = ""
    private var isInFlight = false
    private var flightStartTime = 0L
    private var lastFlightDurationMs = 0L
    private var searchJob: Job? = null
    private var streamingJob: Job? = null
    private var lastJumpTime = 0L
    private var lastConcentricEndTime = 0L
    private var prevHighGTime = 0L  // último instante con G > 1.5 (despegue candidato)

    init {
        api.setApiCallback(object : PolarBleApiCallback() {
            override fun blePowerStateChanged(powered: Boolean) {
                if (!powered) {
                    _connectionState.value = ConnectionState.Error(
                        _connectionState.value.deviceIdOrNull ?: "",
                        "Bluetooth desactivado"
                    )
                }
            }
            override fun deviceConnected(polarDeviceInfo: PolarDeviceInfo) {
                Log.d(TAG, "CONNECTED: ${polarDeviceInfo.deviceId}")
                _connectionState.value = ConnectionState.Connected(polarDeviceInfo.deviceId)
            }
            override fun deviceConnecting(polarDeviceInfo: PolarDeviceInfo) {
                _connectionState.value = ConnectionState.Connecting(polarDeviceInfo.deviceId)
            }
            override fun deviceDisconnected(polarDeviceInfo: PolarDeviceInfo) {
                _connectionState.value = ConnectionState.Disconnected
            }
            override fun disInformationReceived(identifier: String, uuid: java.util.UUID, value: String) {}
            override fun disInformationReceived(identifier: String, disInfo: com.polar.androidcommunications.api.ble.model.DisInfo) {}
            override fun batteryLevelReceived(identifier: String, level: Int) {}
            override fun batteryChargingStatusReceived(identifier: String, state: com.polar.androidcommunications.api.ble.model.gatt.client.ChargeState) {}
            override fun powerSourcesStateReceived(identifier: String, state: com.polar.androidcommunications.api.ble.model.gatt.client.PowerSourcesState) {}
            override fun hrNotificationReceived(identifier: String, sample: com.polar.sdk.api.model.PolarHrData.PolarHrSample) {}
            override fun htsNotificationReceived(identifier: String, data: com.polar.sdk.api.model.PolarHealthThermometerData) {}
            override fun bleSdkFeaturesReadiness(identifier: String, readyFeatures: List<PolarBleSdkFeature>, notReadyFeatures: List<PolarBleSdkFeature>) {}
            override fun bleSdkFeatureReady(identifier: String, feature: PolarBleApi.PolarBleSdkFeature) {}
        })
    }

    fun connect(deviceId: String) {
        val id = deviceId.trim().uppercase()
        _connectionState.value = ConnectionState.Connecting(id)
        try {
            api.connectToDevice(id)
        } catch (e: PolarInvalidArgument) {
            _connectionState.value = ConnectionState.Error(id, "ID invalido: $id")
        }
    }

    fun disconnect() {
        stopStreaming()
        val id = _connectionState.value.deviceIdOrNull
        if (id != null) try { api.disconnectFromDevice(id) } catch (_: Exception) {}
        _connectionState.value = ConnectionState.Disconnected
    }

    fun startDeviceSearch() {
        if (_isSearching.value) return
        _isSearching.value = true
        _discoveredDevices.value = emptyList()
        _connectionState.value = ConnectionState.Scanning
        api.setAutomaticReconnection(true)
        searchJob = scope.launch {
            try {
                api.searchForDevice().collect { device ->
                    _discoveredDevices.update { current ->
                        if (current.none { it.deviceId == device.deviceId }) current + device
                        else current
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Device search error: ${e.message}")
            } finally {
                _isSearching.value = false
                if (_connectionState.value is ConnectionState.Scanning) {
                    _connectionState.value = ConnectionState.Disconnected
                }
            }
        }
    }

    fun stopDeviceSearch() {
        searchJob?.cancel()
        searchJob = null
        _isSearching.value = false
        if (_connectionState.value is ConnectionState.Scanning) _connectionState.value = ConnectionState.Disconnected
    }

    fun reconnect(deviceId: String, attempt: Int) {
        val id = deviceId.trim().uppercase()
        _connectionState.value = ConnectionState.Reconnecting(id, attempt)
        try { api.connectToDevice(id) } catch (_: Exception) {}
    }

    fun startStreaming(deviceId: String, deporte: TipoDeporte) {
        stopStreaming()
        val id = deviceId.trim().uppercase()
        resetBiomechanics(deporte)
        streamingJob = scope.launch {
            supervisorScope {
                launch { runHrStream(id) }
                launch { runEcgStream(id) }
                launch { delay(2000L); runAccStream(id, deporte) }
            }
        }
    }

    fun restartStreaming(deviceId: String, deporte: TipoDeporte) {
        val id = deviceId.trim().uppercase()
        streamingJob?.cancel()
        streamingJob = scope.launch {
            supervisorScope {
                launch { runHrStream(id) }
                launch { runEcgStream(id) }
                launch { delay(2000L); runAccStream(id, deporte) }
            }
        }
        Log.d(TAG, "Streaming reiniciado tras reconexión: $deporte")
    }

    fun startHrOnlyStream(deviceId: String) {
        scope.launch { runHrStream(deviceId.trim().uppercase()) }
    }

    private suspend fun runHrStream(id: String) {
        try {
            api.startHrStreaming(id).collect { data ->
                data.samples.firstOrNull()?.let { sample ->
                    val hr = sample.hr
                    _hrValue.value = hr
                    processRR(sample.rrsMs)
                    hrHistoryList.add(hr)
                    _hrHistory.value = hrHistoryList.toList()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "HR stream error: ${e.message}")
        }
    }

    private suspend fun runEcgStream(id: String) {
        try {
            val settings = api.requestStreamSettings(id, PolarBleApi.PolarDeviceDataType.ECG)
            api.startEcgStreaming(id, settings.maxSettings()).collect { data ->
                val newSamples = data.samples.map { it.timeStamp.toFloat() }
                _ecgSamples.update { (it + newSamples).takeLast(250) }
            }
        } catch (_: Exception) { }
    }

    private suspend fun runAccStream(id: String, deporte: TipoDeporte) {
        var retries = 3
        while (retries-- > 0) {
            try {
                val settings = api.requestStreamSettings(id, PolarBleApi.PolarDeviceDataType.ACC)
                api.startAccStreaming(id, settings.maxSettings()).collect { data ->
                    processAccData(data.samples, deporte)
                }
                break
            } catch (e: Exception) {
                val connectionLost = e is kotlinx.coroutines.CancellationException ||
                    e is java.util.concurrent.CancellationException ||
                    e.message?.contains("cancel", ignoreCase = true) == true ||
                    e.message?.contains("closed", ignoreCase = true) == true ||
                    e.message?.contains("disconnect", ignoreCase = true) == true
                Log.w(TAG, "ACC stream retry ($retries left): ${e.message}")
                if (retries > 0 && !connectionLost) delay(2000L) else break
            }
        }
    }

    fun startEcgStream(id: String) {
        scope.launch { runEcgStream(id.trim().uppercase()) }
    }

    fun startAccStream(id: String, deporte: TipoDeporte) {
        scope.launch { runAccStream(id.trim().uppercase(), deporte) }
    }

    fun stopStreaming() {
        streamingJob?.cancel()
        streamingJob = null
        disposables.clear()
    }

    private fun processRR(rrs: List<Int>) {
        if (rrs.isEmpty()) return
        rrIntervals.addAll(rrs)
        rrs.lastOrNull()?.let { _rrIntervalMs.value = it.toLong() }
        if (rrIntervals.size > 4) {
            val diffSq = rrIntervals.zipWithNext { a, b -> (a - b).toDouble().pow(2) }.sum()
            val rmssdVal = sqrt(diffSq / (rrIntervals.size - 1))
            _rmssd.value = rmssdVal
            val stress = (100.0 - (rmssdVal / 0.8)).coerceIn(0.0, 100.0)
            _hrvStress.value = stress
            if (rrIntervals.size > 50) repeat(rrIntervals.size - 50) { rrIntervals.removeAt(0) }
        }
    }

    private fun processAccData(
        samples: List<com.polar.sdk.api.model.PolarAccelerometerData.PolarAccelerometerDataSample>,
        deporte: TipoDeporte
    ) {
        val lastIdx = samples.size - 1
        samples.forEachIndexed { idx, s ->
            val x = s.x.toFloat()
            val y = s.y.toFloat()
            val z = s.z.toFloat()
            when (deporte) {
                TipoDeporte.FUTBOL -> {
                    val rawMag = sqrt(x * x + y * y + z * z) / 1000.0
                    // Dynamic = net movement (remove 1G gravity baseline → 0 when standing)
                    val dyn = (rawMag - 1.0).coerceAtLeast(0.0)
                    currentMechLoad += dyn * 0.005

                    val now = System.currentTimeMillis()

                    // Sprint: rolling 1s average dynamic > 0.8G
                    sprintBuffer.addLast(dyn)
                    while (sprintBuffer.size > 200) sprintBuffer.removeFirst()
                    val newSprint = sprintBuffer.size >= 100 && sprintBuffer.average() > 0.8
                    if (newSprint && !sprintState) sprintCount++
                    sprintState = newSprint

                    // Seguimiento del último pico de G alto (energía de despegue)
                    if (dyn > 1.5) prevHighGTime = now

                    // Salto: necesita despegue reciente (< 600ms), fase aérea 300-800ms
                    // El sensor de pecho no hace caída libre real, baja a ~0.78G en el aire
                    // Al correr los valles entre zancadas suelen ser < 200ms → mínimo 300ms filtra eso
                    if (rawMag < 0.78 && !isInFlight && (now - prevHighGTime) < 600L) {
                        isInFlight = true; flightStartTime = now
                    } else if (rawMag >= 0.78 && isInFlight) {
                        val flightDur = now - flightStartTime
                        if (flightDur in 300L..800L && now - lastJumpTime > 1500L) {
                            jumpCount++
                            lastJumpTime = now
                        }
                        isInFlight = false
                    } else if (!isInFlight && rawMag >= 0.78) {
                        // No despegue confirmado: asegura que isInFlight queda false
                    }

                    // Asymmetry: rolling 5s mean of X axis
                    val xG = x / 1000.0
                    xBuffer.addLast(xG); xWindowSum += xG
                    if (xBuffer.size > 1000) xWindowSum -= xBuffer.removeFirst()
                    val asymmetry = if (xBuffer.size >= 200) {
                        (kotlin.math.abs(xWindowSum / xBuffer.size) / 0.15 * 100.0).coerceIn(0.0, 100.0).toFloat()
                    } else 0f

                    // Live G-force + status update once per batch
                    if (idx == lastIdx) {
                        _futbolBio.update { it.copy(
                            currentGForce = dyn.toFloat(),
                            isCurrentlySprinting = sprintState,
                            sprintCount = sprintCount,
                            jumpCount = jumpCount,
                            asymmetryScore = asymmetry
                        ) }
                    }

                    // Impact: > 1.8G dinámico (total > 2.8G) con 250ms cooldown
                    // Evita que el trote normal (picos ~1.2G) cuente como impacto
                    if (dyn > 1.8 && (now - lastImpactTime) > 250L) {
                        lastImpactTime = now
                        gPeaksHistory.add(dyn)
                        if (gPeaksHistory.size > 50) gPeaksHistory.removeAt(0)
                        val highImpacts = gPeaksHistory.count { it > 3.0 }.toInt()
                        val maxG = gPeaksHistory.maxOrNull()?.toFloat() ?: 0f
                        val avgG = gPeaksHistory.average().toFloat()
                        val cardioLoad = (_hrValue.value.toFloat() / 190f) * 100f
                        _futbolBio.update { prev ->
                            prev.copy(
                                currentGForce = dyn.toFloat(),
                                isCurrentlySprinting = sprintState,
                                totalImpacts = gPeaksHistory.size,
                                highIntensityImpacts = highImpacts,
                                maxGForce = maxOf(prev.maxGForce, dyn.toFloat()),
                                avgGForce = avgG,
                                mechanicalLoadScore = currentMechLoad.toFloat(),
                                cardiovascularLoadScore = cardioLoad,
                                loadRatio = if (cardioLoad > 0) currentMechLoad.toFloat() / cardioLoad else 0f,
                                sprintCount = sprintCount,
                                jumpCount = jumpCount,
                                asymmetryScore = asymmetry
                            )
                        }
                        _biomechanics.update { it.copy(impacto_fuerza_g = dyn, carga_mecanica_g = currentMechLoad, picos_g_history = gPeaksHistory.toList()) }
                    }
                }
                TipoDeporte.PADEL -> {
                    val rawMag = sqrt(x * x + y * y + z * z) / 1000.0
                    // Dynamic acceleration (subtract gravity): 0 at rest, >0 during movement
                    val dyn = (rawMag - 1.0).coerceAtLeast(0.0)
                    val now = System.currentTimeMillis()

                    // Live X/Y axes update once per batch
                    if (idx == lastIdx) {
                        _padelBio.update { it.copy(
                            currentXmG = x,
                            currentYmG = y,
                            currentDynamicG = dyn.toFloat()
                        ) }
                    }

                    // Smash: dynamic > 1.0G (total > 2G) with 400ms cooldown
                    if (dyn > 1.0 && (now - lastSmashTime) > 400L) {
                        smashCount++
                        lastSmashTime = now
                        val peakDps = (dyn * 150.0).toFloat()
                        _padelBio.update { prev ->
                            prev.copy(
                                currentXmG = x,
                                currentYmG = y,
                                currentDynamicG = dyn.toFloat(),
                                totalSmashes = smashCount,
                                avgRotationDps = ((prev.avgRotationDps * (smashCount - 1) + peakDps) / smashCount),
                                maxRotationDps = maxOf(prev.maxRotationDps, peakDps)
                            )
                        }
                    }
                    _biomechanics.update { it.copy(rotacion_tronco_x = x.toDouble(), rotacion_tronco_y = y.toDouble(), smash_count = smashCount) }
                }
                TipoDeporte.GIMNASIO -> {
                    // Use total magnitude (mG) — works for all exercises regardless of axis
                    val mag = sqrt(x * x + y * y + z * z)
                    val now = System.currentTimeMillis()

                    // Detect new set: >30s without a rep resets the current-set counter
                    if (lastRepTime > 0 && (now - lastRepTime) > 30_000L && !isConcentricPhase) {
                        if (repCount > 0) {
                            setCount++
                            repCount = 0
                            firstRepVelocity = 0.0
                            firstRepVelForLoss = 0f
                        }
                    }

                    // Fase concéntrica: > 1.4G y mínimo 300ms desde fin del rep anterior
                    // Reduce falsos positivos de movimientos cortos/ruido
                    if (mag > 1400f && !isConcentricPhase && (now - lastConcentricEndTime) > 300L) {
                        isConcentricPhase = true
                        concentricStart = now
                    } else if (mag < 1050f && isConcentricPhase) {
                        isConcentricPhase = false
                        lastConcentricEndTime = now
                        val durMs = now - concentricStart
                        val durSec = durMs / 1000.0
                        if (durSec in 0.2..5.0) {
                            lastRepTime = now
                            val velocity = (0.5 / durSec).coerceAtMost(3.0)
                            if (repCount == 0) { firstRepVelocity = velocity; firstRepVelForLoss = velocity.toFloat() }
                            repCount++
                            val velocityLoss = if (firstRepVelocity > 0) ((firstRepVelocity - velocity) / firstRepVelocity * 100.0).toFloat() else 0f
                            val concentricMs = durMs
                            gymRepsList.add(GymRepData(
                                repNumber = repCount,
                                durationMs = durMs,
                                concentricDurationMs = concentricMs,
                                eccentricDurationMs = concentricMs,
                                peakVelocity = velocity.toFloat(),
                                meanVelocity = velocity.toFloat(),
                                velocityLossPct = velocityLoss,
                                hrAtRep = _hrValue.value
                            ))
                            val avgVel = gymRepsList.map { it.meanVelocity }.average().toFloat()
                            val totLoss = if (firstRepVelForLoss > 0 && gymRepsList.isNotEmpty())
                                ((firstRepVelForLoss - gymRepsList.last().meanVelocity) / firstRepVelForLoss * 100f) else 0f
                            val totalSets = setCount + 1
                            _gymBio.value = GymBiomechanics(repCount, totalSets, avgVel, totLoss,
                                if (repCount > 0) (gymRepsList.count { it.velocityLossPct > 20f }.toFloat() / repCount) else 0f,
                                gymRepsList.toList())
                            _biomechanics.update { it.copy(repeticiones = repCount, velocidad_concentrica_promedio = velocity, velocity_drop_warning = velocityLoss > 20f) }
                        }
                    }
                }
            }
        }
    }

    fun startRecording(deviceId: String, exerciseId: String) {
        val id = deviceId.trim().uppercase()
        val internalId = exerciseId.take(8).uppercase()
        currentExerciseId = internalId
        _syncState.value = SyncState.Idle

        scope.launch {
            try {
                // Parar grabacion previa si existe (evita OPERATION_NOT_PERMITTED 106)
                try { api.stopRecording(id) } catch (_: Exception) { }
                delay(400L)
                api.startRecording(id, internalId, RecordingInterval.INTERVAL_1S, SampleType.HR)
                _recordingStatus.value = RecordingStatus(true, internalId)
            } catch (e: Exception) {
                Log.e(TAG, "Start recording error: ${e.message}")
            }
        }
    }

    fun stopRecording(deviceId: String) {
        val id = deviceId.trim().uppercase()
        scope.launch {
            try {
                api.stopRecording(id)
                _recordingStatus.value = RecordingStatus(false, currentExerciseId)
            } catch (e: Exception) {
                _recordingStatus.value = RecordingStatus(false, "")
                Log.e(TAG, "Stop recording error: ${e.message}")
            }
        }
    }

    fun fetchAllExercises(deviceId: String, onResult: (List<ExerciseRecordingData>) -> Unit) {
        val id = deviceId.trim().uppercase()
        _syncState.value = SyncState.Downloading

        scope.launch {
            // H10 necesita stopRecording antes de que listExercises vea la grabacion
            try {
                api.stopRecording(id)
                delay(600L) // Dar tiempo al H10 para finalizar/commitear
                Log.d(TAG, "Recording parada antes de listar ejercicios")
            } catch (_: Exception) {
                // No habia grabacion activa — normal si ya fue parada
            }

            try {
                val entries: List<PolarExerciseEntry> = api.listExercises(id).toList()
                if (entries.isEmpty()) {
                    _syncState.value = SyncState.Idle
                    onResult(emptyList())
                    return@launch
                }

                val results = mutableListOf<ExerciseRecordingData>()
                val total = entries.size
                var fetched = 0

                for (entry in entries) {
                    try {
                        val ex = api.fetchExercise(id, entry)
                        val hrSamples = ex.hrSamples
                        val interval = ex.recordingInterval
                        results.add(ExerciseRecordingData(
                            entry.path,
                            hrSamples,
                            interval,
                            hrSamples.size * interval,
                            if (hrSamples.isNotEmpty()) hrSamples.average().toInt() else 0,
                            hrSamples.maxOrNull() ?: 0,
                            hrSamples.filter { it > 0 }.minOrNull() ?: 0,
                            hrSamples.joinToString(",")
                        ))
                    } catch (_: Exception) { }
                    fetched++
                    if (fetched == total) {
                        _syncState.value = SyncState.Idle
                        onResult(results.toList())
                    }
                }
            } catch (e: Exception) {
                val msg = e.message ?: "Error"
                _syncState.value = SyncState.Error(msg)
                onResult(emptyList())
            }
        }
    }

    fun removeAllExercises(deviceId: String) {
        val id = deviceId.trim().uppercase()
        _syncState.value = SyncState.Clearing

        scope.launch {
            try {
                val entries: List<PolarExerciseEntry> = api.listExercises(id).toList()
                if (entries.isEmpty()) {
                    _syncState.value = SyncState.Success
                    return@launch
                }

                for (entry in entries) {
                    try {
                        api.removeExercise(id, entry)
                    } catch (_: Exception) { }
                }
                _syncState.value = SyncState.Success
            } catch (_: Exception) {
                _syncState.value = SyncState.Success
            }
        }
    }

    private fun resetBiomechanics(deporte: TipoDeporte) {
        rrIntervals.clear(); hrHistoryList.clear(); gymRepsList.clear(); gPeaksHistory.clear()
        sprintBuffer.clear(); xBuffer.clear(); xWindowSum = 0.0
        repCount = 0; setCount = 0; lastRepTime = 0L
        firstRepVelocity = 0.0; firstRepVelForLoss = 0f; currentMechLoad = 0.0
        isConcentricPhase = false; concentricStart = 0L; smashCount = 0; lastSmashTime = 0L
        lastImpactTime = 0L; isInFlight = false; flightStartTime = 0L; lastFlightDurationMs = 0L
        sprintState = false; sprintCount = 0; jumpCount = 0
        lastJumpTime = 0L; lastConcentricEndTime = 0L; prevHighGTime = 0L
        _biomechanics.value = DatosBiomecanicos(deporte.displayName)
        _futbolBio.value = FutbolBiomechanics()
        _padelBio.value = PadelBiomechanics()
        _gymBio.value = GymBiomechanics()
        _hrHistory.value = emptyList()
    }

    // ─── Grabación offline de ACC (para partidos reales sin móvil cerca) ──────
    // API confirmada desde el log del SDK en runtime:
    //   startOfflineRecording(String, PolarDeviceDataType, PolarSensorSetting, PolarRecordingSecret?, Continuation) → suspend
    //   requestOfflineRecordingSettings(String, PolarDeviceDataType, Continuation) → suspend
    //   listOfflineRecordings(String) → Flow<PolarOfflineRecordingEntry>
    //   getOfflineRecord(String, PolarOfflineRecordingEntry, PolarRecordingSecret?, Continuation) → suspend
    //   removeOfflineRecord(String, PolarOfflineRecordingEntry, Continuation) → suspend

    fun startAccOfflineRecording(deviceId: String) {
        val id = deviceId.trim().uppercase()
        scope.launch {
            try {
                val settings = api.requestOfflineRecordingSettings(id, PolarBleApi.PolarDeviceDataType.ACC)
                api.startOfflineRecording(id, PolarBleApi.PolarDeviceDataType.ACC, settings.maxSettings(), null)
                Log.d(TAG, "ACC offline recording iniciado en H10")
            } catch (e: Exception) {
                Log.w(TAG, "H10 no soporta grabacion offline de ACC: ${e.message}")
            }
        }
    }

    suspend fun fetchOfflineAccSamples(deviceId: String): List<com.polar.sdk.api.model.PolarAccelerometerData.PolarAccelerometerDataSample> {
        val id = deviceId.trim().uppercase()
        return try {
            val allEntries: List<PolarOfflineRecordingEntry> = api.listOfflineRecordings(id).toList()
            val accEntries: List<PolarOfflineRecordingEntry> =
                allEntries.filter { it.type == PolarBleApi.PolarDeviceDataType.ACC }
            if (accEntries.isEmpty()) return emptyList()

            val allSamples = mutableListOf<com.polar.sdk.api.model.PolarAccelerometerData.PolarAccelerometerDataSample>()
            for (entry in accEntries) {
                try {
                    val data = api.getOfflineRecord(id, entry, null)
                    if (data is PolarOfflineRecordingData.AccOfflineRecording) {
                        allSamples.addAll(data.data.samples)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Error descargando ACC offline entry: ${e.message}")
                }
            }
            for (entry in accEntries) {
                try { api.removeOfflineRecord(id, entry) } catch (_: Exception) { }
            }
            Log.d(TAG, "ACC offline: ${allSamples.size} muestras del H10")
            allSamples
        } catch (e: Exception) {
            Log.w(TAG, "fetchOfflineAccSamples error: ${e.message}")
            emptyList()
        }
    }

    fun processOfflineAccForFutbol(
        samples: List<com.polar.sdk.api.model.PolarAccelerometerData.PolarAccelerometerDataSample>
    ): FutbolBiomechanics {
        if (samples.isEmpty()) return FutbolBiomechanics()

        var sprintCnt = 0; var jumpCnt = 0; var lastJumpMs = 0L; var prevHighGMs = 0L
        var inFlight = false; var flightMs = 0L; var lastImpactMs = 0L
        var sprintSt = false; var mechLd = 0.0; var maxG = 0f
        val spBuf = ArrayDeque<Double>(); val peaks = mutableListOf<Double>()
        val xBuf = ArrayDeque<Double>(); var xSum = 0.0

        val intervalMs = if (samples.size > 1 && samples[0].timeStamp > 0 && samples[1].timeStamp > 0)
            ((samples[1].timeStamp - samples[0].timeStamp) / 1_000_000L).coerceIn(1L, 100L) else 5L
        val sprintWin = (1000L / intervalMs).toInt().coerceIn(10, 500)

        samples.forEachIndexed { idx, s ->
            val x = s.x.toFloat(); val y = s.y.toFloat(); val z = s.z.toFloat()
            val rawMag = sqrt(x * x + y * y + z * z) / 1000.0
            val dyn = (rawMag - 1.0).coerceAtLeast(0.0)
            val nowMs = if (s.timeStamp > 0L) s.timeStamp / 1_000_000L else idx * intervalMs

            mechLd += dyn * 0.005
            spBuf.addLast(dyn); while (spBuf.size > sprintWin) spBuf.removeFirst()
            val sp = spBuf.size >= sprintWin / 2 && spBuf.average() > 0.8
            if (sp && !sprintSt) sprintCnt++; sprintSt = sp

            if (dyn > 1.5) prevHighGMs = nowMs
            if (rawMag < 0.78 && !inFlight && (nowMs - prevHighGMs) < 600L) {
                inFlight = true; flightMs = nowMs
            } else if (rawMag >= 0.78 && inFlight) {
                val dur = nowMs - flightMs
                if (dur in 300L..800L && nowMs - lastJumpMs > 1500L) { jumpCnt++; lastJumpMs = nowMs }
                inFlight = false
            }
            val xG = x / 1000.0; xBuf.addLast(xG); xSum += xG
            if (xBuf.size > 1000) xSum -= xBuf.removeFirst()
            if (dyn > 1.8 && (nowMs - lastImpactMs) > 250L) {
                lastImpactMs = nowMs; peaks.add(dyn)
                if (peaks.size > 500) peaks.removeAt(0)
                if (dyn.toFloat() > maxG) maxG = dyn.toFloat()
            }
        }
        val asymmetry = if (xBuf.size >= 200)
            (kotlin.math.abs(xSum / xBuf.size) / 0.15 * 100.0).coerceIn(0.0, 100.0).toFloat() else 0f

        return FutbolBiomechanics(
            totalImpacts = peaks.size,
            highIntensityImpacts = peaks.count { it > 3.0 },
            maxGForce = maxG,
            avgGForce = if (peaks.isNotEmpty()) peaks.average().toFloat() else 0f,
            mechanicalLoadScore = mechLd.toFloat(),
            cardiovascularLoadScore = 0f,
            loadRatio = 0f,
            sprintCount = sprintCnt,
            jumpCount = jumpCnt,
            asymmetryScore = asymmetry
        )
    }

    fun destroy() {
        stopStreaming(); scope.cancel()
        try { api.shutDown() } catch (_: Exception) {}
    }
}