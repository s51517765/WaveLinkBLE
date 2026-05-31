package com.example.WaveLinkBLE

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Peripheral
import com.juul.kable.Scanner
import com.juul.kable.peripheral
import com.juul.kable.characteristicOf // 追加
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BleViewModel : ViewModel() {

    var isConnected by mutableStateOf(false)
        private set

    private var currentPeripheral: Peripheral? = null

    var dataPoints1 by mutableStateOf(listOf<Float>())
    var dataPoints2 by mutableStateOf(listOf<Float>())
    var dataPoints3 by mutableStateOf(listOf<Float>())

    fun connectToEsp32() {
        viewModelScope.launch {
            try {
                // 1. スキャン（まずはフィルタなしで最初に見つかったものを取得）
                val scanner = Scanner()
                val advertisement = scanner.advertisements.first()

                // 2. Peripheralの作成と保持
                val peripheral = viewModelScope.peripheral(advertisement)
                currentPeripheral = peripheral

                // 3. 接続
                peripheral.connect()
                isConnected = true

                // 4. データ受信ループ
                // ご提示いただいたUUIDを設定
                val characteristic = characteristicOf(
                    service = "55725ac1-066c-48b5-8700-2d9fb3603c5e",
                    characteristic = "69ddb59c-d601-4ea4-ba83-44f679a670ba"
                )

                // データの監視を開始
                peripheral.observe(characteristic).collect { data ->
                    handleIncomingData(data)
                }

            } catch (e: Exception) {
                // エラー内容をデバッグ出力すると原因が特定しやすいです
                println("BLE Error: ${e.message}")
                isConnected = false
                currentPeripheral = null
            }
        }
    }

    fun handleIncomingData(data: ByteArray) {
        val text = String(data).trim()
        // カンマ区切りで分割
        val parts = text.split(",")

        // 存在する要素だけを更新
        if (parts.size >= 1) {
            parts[0].trim().toFloatOrNull()?.let {
                dataPoints1 = (dataPoints1 + it).takeLast(100) // メモリ節約のため最新100個に制限
            }
        }
        if (parts.size >= 2) {
            parts[1].trim().toFloatOrNull()?.let {
                dataPoints2 = (dataPoints2 + it).takeLast(100)
            }
        }
        if (parts.size >= 3) {
            parts[2].trim().toFloatOrNull()?.let {
                dataPoints3 = (dataPoints3 + it).takeLast(100)
            }
        }
    }

    fun disconnectFromEsp32() {
        viewModelScope.launch {
            try {
                currentPeripheral?.disconnect()
            } catch (e: Exception) {
                println("Disconnect Error: ${e.message}")
            } finally {
                currentPeripheral = null
                isConnected = false
            }
        }
    }
}