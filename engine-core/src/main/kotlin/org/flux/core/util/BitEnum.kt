package org.flux.core.util

interface BitEnum {
    val bit: Int
        get() = 1 shl (this as Enum<*>).ordinal

    operator fun plus(other: BitEnum) = this.bit or other.bit
}

operator fun Int.plus(other: BitEnum) = this or other.bit
