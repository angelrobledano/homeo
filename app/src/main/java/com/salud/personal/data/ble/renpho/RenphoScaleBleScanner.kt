package com.salud.personal.data.ble.renpho

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.salud.personal.data.repository.WeightRepository
import com.salud.personal.domain.model.ProvenanceSource
import com.salud.personal.domain.model.WeightMeasurement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant
import java.util.UUID

/**
 * Escáner BLE en segundo plano para la báscula Renpho Elis 1 (QN-Scale).
 * Escucha los paquetes de broadcast publicitarios y guarda los pesajes estabilizados.
 */
class RenphoScaleBleScanner(
    private val context: Context,
    private val weightRepository: WeightRepository,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _liveMeasurement = MutableStateFlow<QnScaleMeasurement?>(null)
    val liveMeasurement: StateFlow<QnScaleMeasurement?> = _liveMeasurement.asStateFlow()

    private val _lastCapturedMeasurement = MutableStateFlow<WeightMeasurement?>(null)
    val lastCapturedMeasurement: StateFlow<WeightMeasurement?> = _lastCapturedMeasurement.asStateFlow()

    private val parser = QnScalePacketParser()

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            try {
                result ?: return
                val scanRecord = result.scanRecord ?: return

                val payload = extractScalePayload(scanRecord) ?: return
                val parsed = parser.parse(payload) ?: return

                _liveMeasurement.value = parsed

                // Solo persistir si la medición está estabilizada
                if (parsed.isStabilized) {
                    coroutineScope.launch {
                        try {
                            val measurement = WeightMeasurement(
                                source = ProvenanceSource.RENPHO_ELIS_1,
                                externalId = result.device.address,
                                weightKg = parsed.weightKg,
                                impedanceOhms = parsed.impedanceOhms,
                                measuredAt = Instant.now(),
                                importedAt = Instant.now()
                            )
                            weightRepository.saveMeasurement(measurement)
                            _lastCapturedMeasurement.value = measurement
                        } catch (_: Exception) {
                            // Ignorar errores de validación de paquetes anómalos
                        }
                    }
                }
            } catch (_: Exception) {
                // Protección global contra paquetes malformados de periféricos cercanos
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
        }
    }

    private fun extractScalePayload(scanRecord: android.bluetooth.le.ScanRecord): ByteArray? {
        val qnUuid = ParcelUuid(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb"))
        val directServiceData = scanRecord.getServiceData(qnUuid)
        if (directServiceData != null && directServiceData.size >= 6) {
            return directServiceData
        }

        val rawBytes = scanRecord.bytes ?: return null
        var index = 0
        while (index < rawBytes.size) {
            val length = rawBytes[index].toInt() and 0xFF
            if (length == 0 || index + 1 + length > rawBytes.size) break
            val type = rawBytes[index + 1].toInt() and 0xFF
            if (type == 0x16 && length >= 3) {
                val uuid = ((rawBytes[index + 3].toInt() and 0xFF) shl 8) or (rawBytes[index + 2].toInt() and 0xFF)
                if (uuid == 0xFFE0 && length >= 9) {
                    return rawBytes.copyOfRange(index + 4, index + 1 + length)
                }
            }
            index += 1 + length
        }

        return null
    }

    @SuppressLint("MissingPermission")
    fun startScanning() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        if (_isScanning.value) return

        val filters = listOf(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb")))
                .build()
        )

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(filters, settings, scanCallback)
            _isScanning.value = true
        } catch (_: SecurityException) {
            _isScanning.value = false
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScanning() {
        val scanner = bluetoothAdapter?.bluetoothLeScanner ?: return
        if (!_isScanning.value) return

        try {
            scanner.stopScan(scanCallback)
        } catch (_: SecurityException) {
        } finally {
            _isScanning.value = false
        }
    }
}
