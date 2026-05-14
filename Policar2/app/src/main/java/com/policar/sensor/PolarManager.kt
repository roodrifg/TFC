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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val disposables = CompositeDisposable()

    private var _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private var _hrValue = MutableStateFlow(0)
    private var _hrvStress = MutableStateFlow(0.0)
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
    private var firstRepVelocity = 0.0
    private var currentMechLoad = 0.0
    private var isConcentricPhase = false
    private var concentricStart = 0L
    private var smashCount = 0
    private var lastSmashTime = 0L
    private val gPeaksHistory = mutableListOf<Double>()
    private val gymRepsList = mutableListOf<GymRepData>()
    private var firstRepVelForLoss = 0f
    private var currentExerciseId = ""

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
        scope.launch {
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
        _isSearching.value = false
        if (_connectionState.value is ConnectionState.Scanning) _connectionState.value = ConnectionState.Disconnected
    }

    fun reconnect(deviceId: String, attempt: Int) {
        val id = deviceId.trim().uppercase()
        _connectionState.value = ConnectionState.Reconnecting(id, attempt)
        try { api.connectToDevice(id) } catch (_: Exception) {}
    }

    fun startStreaming(deviceId: String, deporte: TipoDeporte) {
        val id = deviceId.trim().uppercase()
        resetBiomechanics(deporte)
        scope.launch {
            try {
                startHrStreamImpl(id)
                startEcgStreamImpl(id)
                startAccStreamImpl(id, deporte)
            } catch (e: Exception) {
                Log.e(TAG, "Streaming error: ${e.message}")
            }
        }
    }

    fun startHrOnlyStream(deviceId: String) {
        scope.launch {
            try { startHrStreamImpl(deviceId.trim().uppercase()) } catch (_: Exception) {}
        }
    }

    private fun startHrStreamImpl(id: String) {
        scope.launch {
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
    }

    private fun startEcgStreamImpl(id: String) {
        scope.launch {
            try {
                val settings = api.requestStreamSettings(id, PolarBleApi.PolarDeviceDataType.ECG)
                api.startEcgStreaming(id, settings.maxSettings()).collect { data ->
                    val newSamples = data.samples.map { sample -> sample.timeStamp.toFloat() }
                    _ecgSamples.update { (it + newSamples).takeLast(250) }
                }
            } catch (_: Exception) { }
        }
    }

    private fun startAccStreamImpl(id: String, deporte: TipoDeporte) {
        scope.launch {
            try {
                val settings = api.requestStreamSettings(id, PolarBleApi.PolarDeviceDataType.ACC)
                api.startAccStreaming(id, settings.maxSettings()).collect { data ->
                    processAccData(data.samples, deporte)
                }
            } catch (_: Exception) { }
        }
    }

    fun startEcgStream(id: String) {
        scope.launch { try { startEcgStreamImpl(id.trim().uppercase()) } catch (_: Exception) {} }
    }

    fun startAccStream(id: String, deporte: TipoDeporte) {
        scope.launch { try { startAccStreamImpl(id.trim().uppercase(), deporte) } catch (_: Exception) {} }
    }

    fun stopStreaming() {
        disposables.clear()
    }

    private fun processRR(rrs: List<Int>) {
        if (rrs.isEmpty()) return
        rrIntervals.addAll(rrs)
        rrs.lastOrNull()?.let { _rrIntervalMs.value = it.toLong() }
        if (rrIntervals.size > 20) {
            val diffSq = rrIntervals.zipWithNext { a, b -> (a - b).toDouble().pow(2) }.sum()
            val rmssd = sqrt(diffSq / (rrIntervals.size - 1))
            val stress = (100.0 - (rmssd / 10.0)).coerceIn(0.0, 100.0)
            _hrvStress.value = stress
            if (rrIntervals.size > 50) repeat(rrIntervals.size - 50) { rrIntervals.removeAt(0) }
        }
    }

    private fun processAccData(
        samples: List<com.polar.sdk.api.model.PolarAccelerometerData.PolarAccelerometerDataSample>,
        deporte: TipoDeporte
    ) {
        samples.forEach { s ->
            val x = s.x.toFloat()
            val y = s.y.toFloat()
            val z = s.z.toFloat()
            when (deporte) {
                TipoDeporte.FUTBOL -> {
                    val mag = sqrt(x * x + y * y + z * z) / 1000.0
                    currentMechLoad += mag * 0.005
                    if (mag > 4.5) {
                        gPeaksHistory.add(mag)
                        if (gPeaksHistory.size > 30) gPeaksHistory.removeAt(0)
                        val highImpacts = gPeaksHistory.count { it > 6.0 }.toInt()
                        val maxG = gPeaksHistory.maxOrNull()?.toFloat() ?: 0f
                        val avgG = gPeaksHistory.average().toFloat()
                        val cardioLoad = (_hrValue.value.toFloat() / 190f) * 100f
                        _futbolBio.value = FutbolBiomechanics(
                            totalImpacts = gPeaksHistory.size,
                            highIntensityImpacts = highImpacts,
                            maxGForce = maxG,
                            avgGForce = avgG,
                            mechanicalLoadScore = currentMechLoad.toFloat(),
                            cardiovascularLoadScore = cardioLoad,
                            loadRatio = if (cardioLoad > 0) currentMechLoad.toFloat() / cardioLoad else 0f
                        )
                        _biomechanics.update { it.copy(impacto_fuerza_g = mag, carga_mecanica_g = currentMechLoad, picos_g_history = gPeaksHistory.toList()) }
                    }
                }
                TipoDeporte.PADEL -> {
                    val lateral = sqrt(x * x + y * y) / 1000.0
                    val now = System.currentTimeMillis()
                    if (lateral > 2.0 && (now - lastSmashTime) > 500L) {
                        smashCount++
                        lastSmashTime = now
                        val avgRot = (lateral * 10.0).toFloat()
                        _padelBio.update { prev ->
                            val newMax = maxOf(prev.maxRotationDps, avgRot)
                            prev.copy(totalSmashes = smashCount, avgRotationDps = ((prev.avgRotationDps * (smashCount - 1) + avgRot) / smashCount), maxRotationDps = newMax)
                        }
                    }
                    _biomechanics.update { it.copy(rotacion_tronco_x = x.toDouble(), rotacion_tronco_y = y.toDouble(), smash_count = smashCount) }
                }
                TipoDeporte.GIMNASIO -> {
                    if (z > 600 && !isConcentricPhase) {
                        isConcentricPhase = true
                        concentricStart = System.currentTimeMillis()
                    } else if (z < -100 && isConcentricPhase) {
                        isConcentricPhase = false
                        val durMs = System.currentTimeMillis() - concentricStart
                        val durSec = durMs / 1000.0
                        if (durSec > 0.15) {
                            val velocity = 0.5 / durSec
                            if (repCount == 0) { firstRepVelocity = velocity; firstRepVelForLoss = velocity.toFloat() }
                            repCount++
                            val velocityLoss = if (firstRepVelocity > 0) ((firstRepVelocity - velocity) / firstRepVelocity * 100.0).toFloat() else 0f
                            val concentricMs = (durSec * 600).toLong()
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
                            val totLoss = if (firstRepVelForLoss > 0 && gymRepsList.isNotEmpty()) ((firstRepVelForLoss - gymRepsList.last().meanVelocity) / firstRepVelForLoss * 100f) else 0f
                            _gymBio.value = GymBiomechanics(repCount, 1, avgVel, totLoss, if (repCount > 0) (gymRepsList.count { it.velocityLossPct > 20f }.toFloat() / repCount) else 0f, gymRepsList.toList())
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
        repCount = 0; firstRepVelocity = 0.0; firstRepVelForLoss = 0f; currentMechLoad = 0.0
        isConcentricPhase = false; concentricStart = 0L; smashCount = 0; lastSmashTime = 0L
        _biomechanics.value = DatosBiomecanicos(deporte.displayName)
        _futbolBio.value = FutbolBiomechanics()
        _padelBio.value = PadelBiomechanics()
        _gymBio.value = GymBiomechanics()
        _hrHistory.value = emptyList()
    }

    fun destroy() {
        stopStreaming(); scope.cancel()
        try { api.shutDown() } catch (_: Exception) {}
    }
}