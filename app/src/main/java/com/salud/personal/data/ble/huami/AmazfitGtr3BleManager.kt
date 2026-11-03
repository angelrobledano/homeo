package com.salud.personal.data.ble.huami

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.os.Build
import android.util.Log
import com.salud.personal.data.ble.huami.auth.AuthResultStatus
import com.salud.personal.data.ble.huami.auth.Huami2021AuthHandshake
import com.salud.personal.data.ble.huami.codec.DecodedMessage
import com.salud.personal.data.ble.huami.codec.Huami2021ChunkedDecoder
import com.salud.personal.data.ble.huami.codec.Huami2021ChunkedEncoder
import com.salud.personal.data.local.entity.SleepSessionEntity
import com.salud.personal.data.local.entity.WorkoutSessionEntity
import com.salud.personal.data.local.entity.WorkoutType
import com.salud.personal.domain.model.DailyActivitySummary
import com.salud.personal.domain.model.DeviceConnectionState
import com.salud.personal.domain.model.HeartRateSample
import com.salud.personal.domain.model.WatchBatteryStatus
import com.salud.personal.domain.training.TrainingLoadCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicBoolean

class AmazfitGtr3BleManager(
    private val context: Context? = null
) : WatchBleManager {

    private val _connectionState = MutableStateFlow(DeviceConnectionState.DISCONNECTED)
    override val connectionState: StateFlow<DeviceConnectionState> = _connectionState.asStateFlow()

    private val _batteryStatus = MutableStateFlow<WatchBatteryStatus?>(null)
    override val batteryStatus: StateFlow<WatchBatteryStatus?> = _batteryStatus.asStateFlow()

    private val _heartRateSamples = MutableSharedFlow<HeartRateSample>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val heartRateSamples: SharedFlow<HeartRateSample> = _heartRateSamples.asSharedFlow()

    private val _dailyActivity = MutableSharedFlow<DailyActivitySummary>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val dailyActivity: SharedFlow<DailyActivitySummary> = _dailyActivity.asSharedFlow()

    private val _completedWorkouts = MutableSharedFlow<WorkoutSessionEntity>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val completedWorkouts: SharedFlow<WorkoutSessionEntity> = _completedWorkouts.asSharedFlow()

    private val _sleepSessions = MutableSharedFlow<SleepSessionEntity>(
        replay = 1,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    override val sleepSessions: SharedFlow<SleepSessionEntity> = _sleepSessions.asSharedFlow()

    internal var bluetoothGatt: BluetoothGatt? = null
    internal var handshake: Huami2021AuthHandshake? = null
    internal val encoder = Huami2021ChunkedEncoder()
    internal val decoder = Huami2021ChunkedDecoder()

    internal var targetMac: String? = null
    internal var currentAuthKeyHex: String? = null

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var authTimeoutJob: Job? = null
    private var authFallbackJob: Job? = null
    private var telemetryJob: Job? = null
    private val authStarted = AtomicBoolean(false)
    private val servicesDiscoveryStarted = AtomicBoolean(false)

    private val writeQueue = ConcurrentLinkedQueue<ByteArray>()
    private val isWriting = AtomicBoolean(false)
    private val servicesDiscovered = AtomicBoolean(false)

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            logD(TAG, "onConnectionStateChange: status=$status, newState=$newState")
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                servicesDiscoveryStarted.set(false)
                authStarted.set(false)
                try {
                    logD(TAG, "Connected to peripheral. Discovering services...")
                    gatt.discoverServices()
                } catch (e: SecurityException) {
                    logE(TAG, "SecurityException discovering services", e)
                    disconnect(keepAuthFailed = false)
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                logD(TAG, "Disconnected from peripheral, status=$status")
                disconnect(keepAuthFailed = false)
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            logD(TAG, "onMtuChanged: mtu=$mtu, status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                encoder.setMTU(mtu)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            logD(TAG, "onServicesDiscovered: status=$status, services=${gatt.services.map { it.uuid }}")
            if (status != BluetoothGatt.GATT_SUCCESS) {
                disconnect(keepAuthFailed = false)
                return
            }
            if (!servicesDiscovered.compareAndSet(false, true)) {
                logD(TAG, "Ignoring duplicate onServicesDiscovered")
                return
            }

            val readNotifyChar = findCharacteristic(gatt, UUID_CHAR_READ_NOTIFY)
            if (readNotifyChar != null) {
                logD(TAG, "Found readNotifyChar in service ${readNotifyChar.service?.uuid}")
                val enabled = enableNotification(gatt, readNotifyChar)
                if (enabled) {
                    authFallbackJob?.cancel()
                    authFallbackJob = managerScope.launch {
                        delay(600)
                        if (authStarted.compareAndSet(false, true)) {
                            logD(TAG, "onDescriptorWrite timeout fallback triggered -> starting auth handshake")
                            val authKey = currentAuthKeyHex
                            if (authKey != null) {
                                startAuthHandshake(gatt, authKey)
                            }
                        }
                    }
                } else {
                    logW(TAG, "Failed to enable notification on readNotifyChar")
                    disconnect(keepAuthFailed = false)
                }
            } else {
                logW(TAG, "readNotifyChar not found in any service!")
                disconnect(keepAuthFailed = false)
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int
        ) {
            val charUuid = descriptor.characteristic?.uuid
            logD(TAG, "onDescriptorWrite: charUuid=$charUuid, status=$status")
            if (status != BluetoothGatt.GATT_SUCCESS) return
            when (charUuid) {
                UUID_CHAR_READ_NOTIFY -> {
                    authFallbackJob?.cancel()
                    if (authStarted.compareAndSet(false, true)) {
                        logD(TAG, "onDescriptorWrite triggered -> starting auth handshake")
                        val authKey = currentAuthKeyHex
                        if (authKey != null) {
                            startAuthHandshake(gatt, authKey)
                        }
                    }
                }
                UUID_CHAR_HEART_RATE_MEASUREMENT -> {
                    enableContinuousHeartRateSampling(gatt)
                    readBatteryLevel(gatt)
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            logD(TAG, "onCharacteristicWrite: uuid=${characteristic.uuid}, status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                processWriteQueue(gatt)
            } else {
                logW(TAG, "onCharacteristicWrite non-success status=$status")
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int
        ) {
            logD(TAG, "onCharacteristicRead (deprecated): uuid=${characteristic.uuid}, status=$status")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val value = getCharacteristicValue(characteristic, null)
                handleCharacteristicData(gatt, characteristic.uuid, value)
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            logD(TAG, "onCharacteristicRead: uuid=${characteristic.uuid}, status=$status, size=${value.size}")
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicData(gatt, characteristic.uuid, value)
            }
        }

        @Deprecated("Deprecated in Java")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = getCharacteristicValue(characteristic, null)
            logD(TAG, "onCharacteristicChanged (deprecated): uuid=${characteristic.uuid}, size=${value.size}")
            handleCharacteristicData(gatt, characteristic.uuid, value)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            logD(TAG, "onCharacteristicChanged: uuid=${characteristic.uuid}, size=${value.size}")
            handleCharacteristicData(gatt, characteristic.uuid, value)
        }
    }

    override fun connect(macAddress: String, authKeyHex: String) {
        if (macAddress.isBlank() || authKeyHex.isBlank()) return
        targetMac = macAddress
        currentAuthKeyHex = authKeyHex
        servicesDiscovered.set(false)
        servicesDiscoveryStarted.set(false)
        authStarted.set(false)
        authTimeoutJob?.cancel()
        authTimeoutJob = null
        authFallbackJob?.cancel()
        authFallbackJob = null

        val ctx = context
        if (ctx == null) {
            _connectionState.value = DeviceConnectionState.DISCONNECTED
            return
        }

        _connectionState.value = DeviceConnectionState.CONNECTING

        try {
            bluetoothGatt?.close()
            bluetoothGatt = null

            val bluetoothManager = ctx.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            val adapter = bluetoothManager?.adapter
            if (adapter == null || !adapter.isEnabled) {
                _connectionState.value = DeviceConnectionState.DISCONNECTED
                return
            }

            val device = adapter.getRemoteDevice(macAddress)
            bluetoothGatt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                device.connectGatt(ctx, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } else {
                device.connectGatt(ctx, false, gattCallback)
            }
        } catch (e: SecurityException) {
            _connectionState.value = DeviceConnectionState.DISCONNECTED
        } catch (e: Exception) {
            _connectionState.value = DeviceConnectionState.DISCONNECTED
        }
    }

    override fun disconnect() {
        disconnect(keepAuthFailed = false)
    }

    fun disconnect(keepAuthFailed: Boolean) {
        servicesDiscovered.set(false)
        servicesDiscoveryStarted.set(false)
        authStarted.set(false)
        authTimeoutJob?.cancel()
        authTimeoutJob = null
        authFallbackJob?.cancel()
        authFallbackJob = null
        telemetryJob?.cancel()
        telemetryJob = null
        writeQueue.clear()
        isWriting.set(false)
        try {
            bluetoothGatt?.disconnect()
            bluetoothGatt?.close()
        } catch (e: SecurityException) {
            // Ignored
        } catch (e: Exception) {
            // Ignored
        } finally {
            bluetoothGatt = null
            handshake = null
            if (!keepAuthFailed || _connectionState.value != DeviceConnectionState.AUTH_FAILED) {
                _connectionState.value = DeviceConnectionState.DISCONNECTED
            }
        }
    }

    fun startAuthHandshake(gatt: BluetoothGatt?, authKeyHex: String) {
        try {
            logD(TAG, "startAuthHandshake: starting with authKey length=${authKeyHex.length}")
            _connectionState.value = DeviceConnectionState.AUTHENTICATING
            val authKey = Huami2021AuthHandshake.parseAuthKey(authKeyHex)
            val hs = Huami2021AuthHandshake(authKey)
            handshake = hs
            encoder.reset()
            decoder.reset()

            val initCmd = hs.start()
            logD(TAG, "startAuthHandshake: initCmd (${initCmd.size} bytes)")
            val chunks = encoder.encode(ENDPOINT_AUTH, initCmd, encrypt = false)
            logD(TAG, "startAuthHandshake: encoded into ${chunks.size} chunks")

            authTimeoutJob?.cancel()
            authTimeoutJob = managerScope.launch {
                delay(12_000)
                if (_connectionState.value == DeviceConnectionState.AUTHENTICATING) {
                    logW(TAG, "Auth handshake timed out after 12s. Peripheral did not respond to challenge. Ensure Zepp app is closed.")
                    _connectionState.value = DeviceConnectionState.AUTH_FAILED
                    disconnect(keepAuthFailed = true)
                }
            }

            if (gatt != null) {
                enqueueChunks(gatt, chunks)
            }
        } catch (e: Exception) {
            logE(TAG, "Exception in startAuthHandshake", e)
            _connectionState.value = DeviceConnectionState.AUTH_FAILED
            disconnect(keepAuthFailed = true)
        }
    }

    fun handleCharacteristicData(gatt: BluetoothGatt?, uuid: UUID, data: ByteArray) {
        logD(TAG, "handleCharacteristicData: uuid=$uuid, len=${data.size}")
        when (uuid) {
            UUID_CHAR_READ_NOTIFY -> {
                val message = decoder.decode(data)
                if (message != null) {
                    logD(TAG, "Decoded message endpoint=0x${message.type.toString(16)}, size=${message.payload.size}")
                    dispatchDecodedMessage(gatt, message)
                }
            }
            UUID_CHAR_HEART_RATE_MEASUREMENT -> {
                val sample = parseHeartRate(data, SOURCE_DEVICE)
                if (sample != null) {
                    _heartRateSamples.tryEmit(sample)
                }
            }
            UUID_CHAR_BATTERY_LEVEL -> {
                if (data.isNotEmpty()) {
                    val battery = parseBatteryLevel(data[0])
                    if (battery != null) {
                        _batteryStatus.value = battery
                    }
                }
            }
        }
    }

    fun dispatchDecodedMessage(gatt: BluetoothGatt?, message: DecodedMessage) {
        val preview = message.payload.take(32).joinToString(" ") { String.format("%02x", it) }
        logD(TAG, "dispatchDecodedMessage: type=0x${message.type.toString(16)}, size=${message.payload.size}, preview=[$preview]")
        when (message.type) {
            ENDPOINT_AUTH -> {
                val currentHandshake = handshake ?: return
                logD(TAG, "Auth payload received, handshake state=${currentHandshake.state}")
                when (currentHandshake.state) {
                    Huami2021AuthHandshake.State.WAITING_CHALLENGE -> {
                        try {
                            logD(TAG, "Handling challenge payload (${message.payload.size} bytes)...")
                            val responseCmd = currentHandshake.handleChallenge(message.payload)
                            logD(TAG, "Challenge handled, response command size=${responseCmd.size}")
                            val chunks = encoder.encode(ENDPOINT_AUTH, responseCmd, encrypt = false)
                            logD(TAG, "Response chunks: ${chunks.size}")
                            if (gatt != null) {
                                enqueueChunks(gatt, chunks)
                            }
                        } catch (e: Exception) {
                            logE(TAG, "Error handling challenge", e)
                            _connectionState.value = DeviceConnectionState.AUTH_FAILED
                            disconnect(keepAuthFailed = true)
                        }
                    }
                    Huami2021AuthHandshake.State.WAITING_RESPONSE -> {
                        val status = currentHandshake.handleResponse(message.payload)
                        logD(TAG, "Auth response handled, status=$status")
                        if (status == AuthResultStatus.SUCCESS) {
                            authTimeoutJob?.cancel()
                            authTimeoutJob = null
                            _connectionState.value = DeviceConnectionState.CONNECTED
                            val sessionKey = currentHandshake.sessionKey ?: return
                            val sequenceNumber = currentHandshake.sequenceNumber ?: return
                            encoder.setEncryptionParameters(sequenceNumber, sessionKey)
                            decoder.setEncryptionParameters(sessionKey)

                            if (gatt != null) {
                                enableHeartRateNotification(gatt)
                                startTelemetrySync(gatt)
                            }
                        } else {
                            _connectionState.value = DeviceConnectionState.AUTH_FAILED
                            disconnect(keepAuthFailed = true)
                        }
                    }
                    else -> { /* Ignore unexpected state */ }
                }
            }
            ENDPOINT_BATTERY -> {
                val battery = parseBatteryPayload(message.payload)
                if (battery != null) {
                    _batteryStatus.value = battery
                }
            }
            ENDPOINT_STEPS, ENDPOINT_REALTIME_ACTIVITY -> {
                val activity = parseActivityPayload(message.payload, SOURCE_DEVICE)
                if (activity != null) {
                    _dailyActivity.tryEmit(activity)
                }
            }
            ENDPOINT_WORKOUT_SUMMARY -> {
                val workout = parseWorkoutSummaryPayload(message.payload)
                if (workout != null) {
                    _completedWorkouts.tryEmit(workout)
                }
            }
            ENDPOINT_SLEEP_SUMMARY -> {
                val sleep = parseSleepSummaryPayload(message.payload)
                if (sleep != null) {
                    _sleepSessions.tryEmit(sleep)
                }
            }
        }
    }

    private fun findCharacteristic(gatt: BluetoothGatt, uuid: UUID): BluetoothGattCharacteristic? {
        gatt.getService(UUID_SERVICE_HUAMI)?.getCharacteristic(uuid)?.let { return it }
        gatt.getService(UUID_SERVICE_MIBAND)?.getCharacteristic(uuid)?.let { return it }
        for (service in gatt.services) {
            val char = service.getCharacteristic(uuid)
            if (char != null) return char
        }
        return null
    }

    private fun enableHeartRateNotification(gatt: BluetoothGatt) {
        val hrChar = findCharacteristic(gatt, UUID_CHAR_HEART_RATE_MEASUREMENT)
        if (hrChar != null) {
            val enabled = enableNotification(gatt, hrChar)
            if (!enabled) {
                enableContinuousHeartRateSampling(gatt)
                readBatteryLevel(gatt)
            }
        } else {
            enableContinuousHeartRateSampling(gatt)
            readBatteryLevel(gatt)
        }
    }

    private fun enableContinuousHeartRateSampling(gatt: BluetoothGatt) {
        val hrControlChar = findCharacteristic(gatt, UUID_CHAR_HEART_RATE_CONTROL_POINT)
        if (hrControlChar != null) {
            // 0x14 = COMMAND_SET_PERIODIC_HR_INTERVAL, 0x01 = 1 minute interval
            val command = byteArrayOf(0x14, 0x01)
            val success = writeCharacteristicData(gatt, hrControlChar, command)
            logD(TAG, "Sent HR continuous sampling command to 0x2A39, success=$success")
        } else {
            logD(TAG, "HR Control Point (0x2A39) not exposed by peripheral")
        }
    }

    private fun startTelemetrySync(gatt: BluetoothGatt) {
        telemetryJob?.cancel()
        telemetryJob = managerScope.launch {
            // Initial activity request upon connection
            delay(1500)
            requestRealtimeActivity(gatt)

            // Periodic telemetry sync loop every 60 seconds
            while (_connectionState.value == DeviceConnectionState.CONNECTED) {
                delay(60_000)
                if (_connectionState.value == DeviceConnectionState.CONNECTED) {
                    requestRealtimeActivity(gatt)
                    readBatteryLevel(gatt)
                }
            }
        }
    }

    fun requestRealtimeActivity(gatt: BluetoothGatt?) {
        if (gatt == null || _connectionState.value != DeviceConnectionState.CONNECTED) return
        try {
            // Huami Zepp OS realtime activity command: [0x04, 0x01] or query [0x01]
            val queryPayload = byteArrayOf(0x04, 0x01)
            val chunks = encoder.encode(ENDPOINT_REALTIME_ACTIVITY, queryPayload, encrypt = true)
            logD(TAG, "Requesting realtime activity on endpoint 0x16 (${chunks.size} chunks)")
            enqueueChunks(gatt, chunks)
        } catch (e: Exception) {
            logE(TAG, "Error requesting realtime activity", e)
        }
    }

    private fun readBatteryLevel(gatt: BluetoothGatt) {
        val batteryChar = findCharacteristic(gatt, UUID_CHAR_BATTERY_LEVEL)
        if (batteryChar != null) {
            try {
                gatt.readCharacteristic(batteryChar)
            } catch (e: SecurityException) {
                // Ignored
            }
        }
    }

    private fun enableNotification(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic
    ): Boolean {
        return try {
            val notifySuccess = gatt.setCharacteristicNotification(characteristic, true)
            logD(TAG, "setCharacteristicNotification for ${characteristic.uuid} = $notifySuccess")
            if (!notifySuccess) return false
            val descriptor = characteristic.getDescriptor(UUID_DESCRIPTOR_CCCD)
            logD(TAG, "descriptor CCCD for ${characteristic.uuid} = $descriptor")
            if (descriptor == null) return false
            val writeSuccess = writeDescriptorValue(gatt, descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            logD(TAG, "writeDescriptorValue for ${characteristic.uuid} = $writeSuccess")
            writeSuccess
        } catch (e: SecurityException) {
            logE(TAG, "SecurityException in enableNotification", e)
            false
        }
    }

    private fun enqueueChunks(gatt: BluetoothGatt, chunks: List<ByteArray>) {
        writeQueue.addAll(chunks)
        if (isWriting.compareAndSet(false, true)) {
            writeNextChunk(gatt)
        }
    }

    private fun processWriteQueue(gatt: BluetoothGatt) {
        if (!writeNextChunk(gatt)) {
            isWriting.set(false)
            if (!writeQueue.isEmpty() && isWriting.compareAndSet(false, true)) {
                writeNextChunk(gatt)
            }
        }
    }

    private fun writeNextChunk(gatt: BluetoothGatt): Boolean {
        val nextChunk = writeQueue.poll() ?: return false
        val writeChar = findCharacteristic(gatt, UUID_CHAR_WRITE)
        if (writeChar == null) {
            logW(TAG, "writeChar not found in any service!")
            writeQueue.clear()
            isWriting.set(false)
            return false
        }

        val success = writeCharacteristicData(gatt, writeChar, nextChunk)
        logD(TAG, "writeNextChunk (${nextChunk.size} bytes) result=$success, remainingInQueue=${writeQueue.size}")
        if (!success) {
            writeQueue.clear()
            isWriting.set(false)
            return false
        }
        return true
    }

    @Suppress("DEPRECATION")
    private fun writeCharacteristicData(
        gatt: BluetoothGatt,
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ): Boolean {
        return try {
            val writeType = if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) != 0) {
                BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            } else {
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            }
            logD(TAG, "writeCharacteristicData: uuid=${characteristic.uuid}, properties=0x${characteristic.properties.toString(16)}, writeType=$writeType")
            val success = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    data,
                    writeType
                ) == 0
            } else {
                characteristic.value = data
                characteristic.writeType = writeType
                gatt.writeCharacteristic(characteristic)
            }
            logD(TAG, "writeCharacteristicData uuid=${characteristic.uuid}, len=${data.size}, success=$success")
            success
        } catch (e: SecurityException) {
            logE(TAG, "SecurityException in writeCharacteristicData", e)
            false
        } catch (e: Exception) {
            logE(TAG, "Exception in writeCharacteristicData", e)
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun writeDescriptorValue(
        gatt: BluetoothGatt,
        descriptor: BluetoothGattDescriptor,
        value: ByteArray
    ): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(descriptor, value) == 0
            } else {
                descriptor.value = value
                gatt.writeDescriptor(descriptor)
            }
        } catch (e: SecurityException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    @Suppress("DEPRECATION")
    private fun getCharacteristicValue(
        characteristic: BluetoothGattCharacteristic,
        value: ByteArray?
    ): ByteArray {
        return value ?: characteristic.value ?: ByteArray(0)
    }

    companion object {
        private const val TAG = "AmazfitGtr3BleManager"
        const val SOURCE_DEVICE = "Amazfit GTR 3"

        private fun logD(tag: String, message: String) {
            try {
                Log.d(tag, message)
            } catch (e: RuntimeException) {
                // JVM test fallback
            }
        }

        private fun logW(tag: String, message: String) {
            try {
                Log.w(tag, message)
            } catch (e: RuntimeException) {
                // JVM test fallback
            }
        }

        private fun logE(tag: String, message: String, throwable: Throwable? = null) {
            try {
                if (throwable != null) Log.e(tag, message, throwable) else Log.e(tag, message)
            } catch (e: RuntimeException) {
                // JVM test fallback
            }
        }

        val UUID_SERVICE_HUAMI: UUID = UUID.fromString("0000fee1-0000-1000-8000-00805f9b34fb")
        val UUID_SERVICE_MIBAND: UUID = UUID.fromString("0000fee0-0000-1000-8000-00805f9b34fb")
        val UUID_CHAR_WRITE: UUID = UUID.fromString("00000016-0000-3512-2118-0009af100700")
        val UUID_CHAR_READ_NOTIFY: UUID = UUID.fromString("00000017-0000-3512-2118-0009af100700")

        val UUID_SERVICE_BATTERY: UUID = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
        val UUID_CHAR_BATTERY_LEVEL: UUID = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

        val UUID_SERVICE_HEART_RATE: UUID = UUID.fromString("0000180d-0000-1000-8000-00805f9b34fb")
        val UUID_CHAR_HEART_RATE_MEASUREMENT: UUID = UUID.fromString("00002a37-0000-1000-8000-00805f9b34fb")

        val UUID_DESCRIPTOR_CCCD: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

        const val ENDPOINT_AUTH: Short = 0x0082
        const val ENDPOINT_BATTERY: Short = 0x0010
        const val ENDPOINT_STEPS: Short = 0x0007
        const val ENDPOINT_REALTIME_ACTIVITY: Short = 0x0016
        const val ENDPOINT_HEART_RATE_CONTROL: Short = 0x000a
        const val ENDPOINT_WORKOUT_SUMMARY: Short = 0x0015
        const val ENDPOINT_SLEEP_SUMMARY: Short = 0x0012

        val UUID_CHAR_HEART_RATE_CONTROL_POINT: UUID = UUID.fromString("00002a39-0000-1000-8000-00805f9b34fb")

        fun parseHeartRate(
            data: ByteArray,
            sourceDevice: String = SOURCE_DEVICE,
            timestamp: Instant = Instant.now()
        ): HeartRateSample? {
            if (data.size < 2) return null
            val flags = data[0].toInt() and 0xFF
            val is16Bit = (flags and 0x01) != 0

            val bpm = if (is16Bit) {
                if (data.size < 3) return null
                (data[1].toInt() and 0xFF) or ((data[2].toInt() and 0xFF) shl 8)
            } else {
                data[1].toInt() and 0xFF
            }

            if (bpm !in 20..300) return null
            return try {
                HeartRateSample(timestamp = timestamp, bpm = bpm, sourceDevice = sourceDevice)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun parseBatteryLevel(
            percentageByte: Byte,
            timestamp: Instant = Instant.now()
        ): WatchBatteryStatus? {
            val percentage = percentageByte.toInt() and 0xFF
            if (percentage !in 0..100) return null
            return try {
                WatchBatteryStatus(
                    percentage = percentage,
                    isCharging = false,
                    timestamp = timestamp
                )
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun parseBatteryPayload(
            payload: ByteArray,
            timestamp: Instant = Instant.now()
        ): WatchBatteryStatus? {
            if (payload.isEmpty()) return null
            val (level, isCharging) = when {
                payload.size >= 2 && (payload[0] == 0x10.toByte() || payload[0] == 0x01.toByte()) -> {
                    val p = payload[1].toInt() and 0xFF
                    val ch = if (payload.size > 2) payload[2].toInt() != 0 else false
                    Pair(p, ch)
                }
                payload.size >= 2 -> {
                    val p = payload[0].toInt() and 0xFF
                    val ch = payload[1].toInt() != 0
                    Pair(p, ch)
                }
                else -> {
                    Pair(payload[0].toInt() and 0xFF, false)
                }
            }

            if (level !in 0..100) return null
            return try {
                WatchBatteryStatus(
                    percentage = level,
                    isCharging = isCharging,
                    timestamp = timestamp
                )
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun parseActivityPayload(
            payload: ByteArray,
            sourceDevice: String = SOURCE_DEVICE,
            date: LocalDate = LocalDate.now()
        ): DailyActivitySummary? {
            if (payload.size < 4) return null
            val offset = when {
                payload.size >= 15 && payload[0] == 0x04.toByte() && payload[1] == 0x01.toByte() && payload[2] == 0x0c.toByte() -> 3
                payload.size >= 13 && (payload[0] == 0x10.toByte() || payload[0] == 0x01.toByte()) -> 1
                else -> 0
            }

            val remaining = payload.size - offset
            if (remaining < 4) return null

            fun readIntLe(off: Int): Int {
                val b0 = payload[off].toInt() and 0xFF
                val b1 = payload[off + 1].toInt() and 0xFF
                val b2 = payload[off + 2].toInt() and 0xFF
                val b3 = payload[off + 3].toInt() and 0xFF
                return (b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24)).coerceAtLeast(0)
            }

            val steps = readIntLe(offset)
            val distance = if (remaining >= 8) readIntLe(offset + 4) else 0
            val calories = if (remaining >= 12) readIntLe(offset + 8) else 0

            return try {
                DailyActivitySummary(
                    date = date,
                    steps = steps,
                    distanceMeters = distance,
                    caloriesBurned = calories,
                    sourceDevice = sourceDevice
                )
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun parseWorkoutSummaryPayload(
            payload: ByteArray,
            now: Instant = Instant.now()
        ): WorkoutSessionEntity? {
            // Zepp OS endpoint 0x15 emits workout events and summaries.
            // Minimum payload length for a summary: 8 bytes (type, start, duration, hr)
            if (payload.size < 8) return null

            fun readIntLe(off: Int): Int {
                val b0 = payload[off].toInt() and 0xFF
                val b1 = payload[off + 1].toInt() and 0xFF
                val b2 = payload[off + 2].toInt() and 0xFF
                val b3 = payload[off + 3].toInt() and 0xFF
                return (b0 or (b1 shl 8) or (b2 shl 16) or (b3 shl 24))
            }

            fun readShortLe(off: Int): Int {
                val b0 = payload[off].toInt() and 0xFF
                val b1 = payload[off + 1].toInt() and 0xFF
                return (b0 or (b1 shl 8))
            }

            val sportTypeByte = payload[0].toInt() and 0xFF
            // Sport types in Zepp OS: 0x01/0x02 = Run/Walk, 0x07 = CrossFit/Fitness/HIIT, etc.
            val (workoutType, workoutName) = when (sportTypeByte) {
                0x01, 0x02 -> Pair(WorkoutType.ENDURANCE_CARDIO, "Carrera / Caminata")
                0x07, 0x10, 0x15 -> Pair(WorkoutType.CROSSFIT_WOD, "Crosstraining WOD")
                else -> Pair(WorkoutType.CROSSFIT_WOD, "Entrenamiento GTR 3")
            }

            val durationSeconds = readIntLe(1).coerceAtLeast(30)
            val durationMinutes = kotlin.math.round((durationSeconds / 60.0) * 10.0) / 10.0

            val avgHr = if (payload.size >= 6) (payload[5].toInt() and 0xFF).takeIf { it in 40..240 } else null
            val maxHr = if (payload.size >= 7) (payload[6].toInt() and 0xFF).takeIf { it in 40..250 } else null
            val calories = if (payload.size >= 9) readShortLe(7) else null

            val calculatedTrimp = if (avgHr != null) {
                TrainingLoadCalculator.calculateBanisterTrimp(
                    durationMinutes = durationMinutes,
                    avgHr = avgHr.toDouble(),
                    restHr = 50.0,
                    maxHr = 190.0
                )
            } else null

            val startTime = now.minusSeconds(durationSeconds.toLong())

            return WorkoutSessionEntity(
                type = workoutType,
                name = workoutName,
                startTime = startTime,
                endTime = now,
                durationMinutes = durationMinutes,
                avgHeartRate = avgHr,
                maxHeartRate = maxHr,
                trimp = calculatedTrimp,
                caloriesBurned = calories,
                rpe = 8,
                notes = "Sincronizado automáticamente desde Amazfit GTR 3"
            )
        }

        fun parseSleepSummaryPayload(
            payload: ByteArray,
            date: LocalDate = LocalDate.now(),
            sourceDevice: String = SOURCE_DEVICE
        ): SleepSessionEntity? {
            // Zepp OS endpoint 0x12 emits sleep summaries.
            // Minimum payload length: 8 bytes (deep, light, rem, awake in LE short)
            if (payload.size < 8) return null

            fun readShortLe(off: Int): Int {
                val b0 = payload[off].toInt() and 0xFF
                val b1 = payload[off + 1].toInt() and 0xFF
                return (b0 or (b1 shl 8))
            }

            val deepSleep = readShortLe(0).coerceIn(0, 720)
            val lightSleep = readShortLe(2).coerceIn(0, 720)
            val remSleep = readShortLe(4).coerceIn(0, 720)
            val awake = readShortLe(6).coerceIn(0, 720)

            val totalSleep = deepSleep + lightSleep + remSleep
            if (totalSleep <= 0) return null

            val totalInBed = totalSleep + awake
            val efficiency = if (totalInBed > 0) {
                ((totalSleep.toDouble() / totalInBed) * 100).toInt().coerceIn(10, 100)
            } else 85

            val now = Instant.now()
            val start = now.minusSeconds((totalInBed * 60).toLong())

            return SleepSessionEntity(
                date = date,
                startTime = start,
                endTime = now,
                deepSleepMinutes = deepSleep,
                lightSleepMinutes = lightSleep,
                remSleepMinutes = remSleep,
                awakeMinutes = awake,
                totalSleepMinutes = totalSleep,
                sleepEfficiencyScore = efficiency,
                sourceDevice = sourceDevice
            )
        }
    }
}
