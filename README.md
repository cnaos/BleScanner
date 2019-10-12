# BleScanner

## これは何？

<img src="https://github.com/cnaos/picture/raw/master/BleScanner/device_list02.png" width="25%"/><img src="https://github.com/cnaos/picture/raw/master/BleScanner/device_detail.png" width="25%"/>

googleの[Android BluetoothLeGatt Sample](https://github.com/android/connectivity-samples/tree/master/BluetoothLeGatt)
と、
HIRAMINEさんの[BLE通信ソフトを作る( Android Studio 2.3.3 + RN4020 )](https://www.hiramine.com/programming/blecommunicator/index.html)
をベースに、
以下のライブラリを組み込んでみたサンプルです。

* Able
  * https://github.com/JuulLabs-OSS/able
  * AndroidのBluetoothLEフレームワークをKotlinのcoroutineで扱えるようにするためのフレームワーク
* Peko
  * https://github.com/deva666/Peko
  * Android PermissionsをKotlin Coroutineまたは、LiveDataで扱えるようにするためのライブラリ
* android-identicons
  * https://github.com/lelloman/android-identicons
  * Ideticonを生成するためのライブラリ


## できること

* Bluetooth LEデバイスのスキャン
* Bluetooth LEデバイスのGATTサービスの一覧の表示
* Bluetooth LEデバイスのGATTキャラクタリスティック（Characteristic）の表示

