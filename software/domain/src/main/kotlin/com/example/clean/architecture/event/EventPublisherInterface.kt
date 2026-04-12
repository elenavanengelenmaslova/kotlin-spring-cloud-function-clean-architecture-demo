package com.example.clean.architecture.event

fun interface EventPublisherInterface {
    fun publishEvent(source: String, detailType: String, detail: String)
}
