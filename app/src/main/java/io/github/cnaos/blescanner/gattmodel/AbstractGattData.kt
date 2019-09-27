package io.github.cnaos.blescanner.gattmodel

abstract class AbstractGattData(val uuid: String) {
    abstract fun dataString(): String

    override fun toString(): String {
        return "$uuid: ${dataString()}"
    }
}