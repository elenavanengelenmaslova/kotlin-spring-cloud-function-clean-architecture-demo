package com.example.clean.architecture.model

data class Pet(
    val id: Long?,
    val name: String?,
    val status: String?,
    val photoUrls: List<String>?,
    val tags: List<String>?
)
