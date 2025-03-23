package com.example.clean.architecture.persistence

interface WireMockMappingRepository {
    fun saveMapping(id: String, content: String): String
    fun getMapping(id: String): String?
    fun deleteMapping(id: String)
    fun listMappings(): List<String>
}
