package com.example.clean.architecture.azure.persistence

import com.azure.storage.blob.BlobContainerClient
import com.example.clean.architecture.persistence.WireMockMappingRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Repository
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

@Repository
@Primary
class AzureBlobStorageWireMockMappingRepository(
    private val containerClient: BlobContainerClient
) : WireMockMappingRepository {

    override fun saveMapping(id: String, content: String): String {
        logger.info { "Saving mapping with id: $id" }
        val blobClient = containerClient.getBlobClient(id)
        blobClient.upload(content.byteInputStream(), true)
        return blobClient.blobUrl
    }

    override fun getMapping(id: String): String? {
        logger.info { "Getting mapping with id: $id" }
        val blobClient = containerClient.getBlobClient(id)
        return if (blobClient.exists()) {
            blobClient.downloadContent().toBytes().toString(StandardCharsets.UTF_8)
        } else {
            logger.info { "Mapping with id: $id not found" }
            null
        }
    }

    override fun deleteMapping(id: String) {
        logger.info { "Deleting mapping with id: $id" }
        val blobClient = containerClient.getBlobClient(id)
        if (blobClient.exists()) {
            blobClient.delete()
        } else {
            logger.info { "Mapping with id: $id not found, nothing to delete" }
        }
    }

    override fun listMappings(): List<String> {
        logger.info { "Listing all mappings" }
        return containerClient.listBlobs()
            .map { it.name }
            .toList()
    }
}
