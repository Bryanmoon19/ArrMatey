package com.dnfapps.arrmatey.arr.api.client

import com.dnfapps.arrmatey.arr.api.model.IndexerStatus
import com.dnfapps.arrmatey.arr.api.model.ProwlarrGrabPayload
import com.dnfapps.arrmatey.arr.api.model.ProwlarrIndexer
import com.dnfapps.arrmatey.arr.api.model.ProwlarrSearchResult
import com.dnfapps.arrmatey.client.NetworkResult
import com.dnfapps.arrmatey.client.safeGet
import com.dnfapps.arrmatey.client.safePost
import com.dnfapps.arrmatey.instances.model.Instance
import io.ktor.client.HttpClient
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent

class ProwlarrClient(
    val instance: Instance,
    private val httpClient: HttpClient
) : KoinComponent {

    private val baseUrl: String
        get() {
            val cleanUrl = instance.getEffectiveBaseUrl().trim().trimEnd('|', '/', ' ')
            val apiBase = instance.type.apiBase.trim().trimStart('/', ' ')
            return "$cleanUrl/$apiBase"
        }

    suspend fun testConnection(): NetworkResult<Unit> {
        val url = "$baseUrl/${instance.type.testEndpoint}"
        println("[ProwlarrClient] testConnection() URL: $url")
        return httpClient.safeGet(url)
    }

    suspend fun getIndexers(): NetworkResult<List<ProwlarrIndexer>> {
        val url = "$baseUrl/indexer"
        println("[ProwlarrClient] getIndexers() URL: $url")
        println("[ProwlarrClient] instance.url: ${instance.url}")
        println("[ProwlarrClient] instance.apiKey: ${instance.apiKey.take(4)}...")
        return httpClient.safeGet(url)
    }

    suspend fun getIndexerStatus(): NetworkResult<List<IndexerStatus>> =
        httpClient.safeGet("$baseUrl/indexerStatus")

    suspend fun search(
        query: String,
        type: String = "search",
        categories: List<Int> = emptyList(),
        indexerIds: List<Long> = emptyList()
    ): NetworkResult<List<ProwlarrSearchResult>> =
        httpClient.safeGet("$baseUrl/search") {
            url {
                parameters.append("query", query)
                parameters.append("type", type)
                if (categories.isNotEmpty()) {
                    parameters.append("categories", categories.joinToString(","))
                }
                if (indexerIds.isNotEmpty()) {
                    parameters.append("indexerIds", indexerIds.joinToString(","))
                }
            }
        }

    suspend fun grab(guid: String, indexerId: Long): NetworkResult<ProwlarrSearchResult> =
        httpClient.safePost("$baseUrl/release") {
            contentType(ContentType.Application.Json)
            setBody(ProwlarrGrabPayload(guid = guid, indexerId = indexerId.toInt()))
        }
}
