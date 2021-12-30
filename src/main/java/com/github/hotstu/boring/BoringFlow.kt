package com.github.hotstu.boring

interface BoringFlow<T: Any?> {
    fun onChange(current: T)
}