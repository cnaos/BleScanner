# BleScanner

## これは何？

![BleScanner](https://github.com/cnaos/picture/raw/master/BleScanner/device_list.png)

![BleScanner](https://github.com/cnaos/picture/raw/master/BleScanner/device_detail.png)

googleの[Android BluetoothLeGatt Sample](https://github.com/android/connectivity-samples/tree/master/BluetoothLeGatt)をベースに、
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

