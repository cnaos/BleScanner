package io.github.cnaos.blescanner.gatt

import java.text.MessageFormat
import java.util.*

/**
 * Gatt関係のUUIDを扱いやすくするためのクラス
 */
abstract class AbstractGattShortUUID(shortUUID: String, uuidTemplate: String) {
    /**
     * 4桁のUUID
     */
    val shortUUID: String

    /**
     * UUID全部
     */
    val fullUUID: String = MessageFormat.format(uuidTemplate, shortUUID).toLowerCase(Locale.ROOT)

    init {
        require(shortUUID.matches(Regex("[0-9a-fA-F]{4}"))) {
            "invalid short UUID"
        }
        this.shortUUID = shortUUID.toLowerCase(Locale.ROOT)
    }


    override fun toString(): String {
        return shortUUID
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AbstractGattShortUUID

        if (fullUUID != other.fullUUID) return false

        return true
    }

    override fun hashCode(): Int {
        return fullUUID.hashCode()
    }
}
