package com.dnfapps.arrmatey.instances.repository

import com.dnfapps.arrmatey.arr.api.client.HttpClientFactory
import com.dnfapps.arrmatey.database.InstanceRepository
import com.dnfapps.arrmatey.downloads.repository.DownloadRepository
import com.dnfapps.arrmatey.instances.model.Instance
import com.dnfapps.arrmatey.instances.model.InstanceType
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class InstanceManager(
    private val instanceRepository: InstanceRepository,
    private val httpClientFactory: HttpClientFactory
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _instanceRepositories =
        MutableStateFlow<Map<Long, InstanceScopedRepository>>(emptyMap())
    val instanceRepositories: StateFlow<Map<Long, InstanceScopedRepository>> = _instanceRepositories

    private val _downloadRepositories =
        MutableStateFlow<Map<Long, DownloadRepository>>(emptyMap())
    val downloadRepositories: StateFlow<Map<Long, DownloadRepository>> = _downloadRepositories

    init {
        observeInstances()
    }

    private fun observeInstances() {
        scope.launch {
            instanceRepository.observeAllInstances()
                .collect { instances ->
                    updateRepositories(instances)
                }
        }
    }

    private fun updateRepositories(instances: List<Instance>) {
        val currentRepos = _instanceRepositories.value.toMutableMap()
        val currentDownloadRepos = _downloadRepositories.value.toMutableMap()
        val instanceIds = instances.map { it.id }.toSet()

        currentRepos.keys
            .filterNot { it in instanceIds }
            .forEach { instanceId ->
                currentRepos.remove(instanceId)
            }
        
        currentDownloadRepos.keys
            .filterNot { it in instanceIds }
            .forEach { instanceId ->
                currentDownloadRepos.remove(instanceId)
            }

        instances.forEach { instance ->
            if (instance.type == InstanceType.QBittorrent || instance.type == InstanceType.Sabnzbd) {
                if (!currentDownloadRepos.containsKey(instance.id)) {
                    val httpClient = httpClientFactory.create(instance)
                    currentDownloadRepos[instance.id] = DownloadRepository(instance, httpClient)
                }
            } else if (!currentRepos.containsKey(instance.id)) {
                val httpClient = httpClientFactory.create(instance)
                currentRepos[instance.id] = createScopedRepository(instance, httpClient)
            }
        }

        _instanceRepositories.value = currentRepos
        _downloadRepositories.value = currentDownloadRepos
    }

    private fun createScopedRepository(instance: Instance, httpClient: HttpClient): InstanceScopedRepository {
        return when (instance.type) {
//            InstanceType.Seerr -> SeerrInstanceRepository(instance, httpClient)
            InstanceType.Sonarr,
            InstanceType.Radarr,
            InstanceType.Lidarr,
            InstanceType.Prowlarr -> ArrInstanceRepository(instance, httpClient)
            InstanceType.QBittorrent, InstanceType.Sabnzbd -> throw IllegalArgumentException("Not a scoped repository type")
        }
    }

    fun getArrRepository(instanceId: Long): ArrInstanceRepository? =
        _instanceRepositories.value[instanceId] as? ArrInstanceRepository?

    fun getSeerrRepository(instanceId: Long): SeerrInstanceRepository? =
        _instanceRepositories.value[instanceId] as? SeerrInstanceRepository

    fun getDownloadRepository(instanceId: Long): DownloadRepository? =
        _downloadRepositories.value[instanceId]

    fun getSelectedDownloadRepository(type: InstanceType): Flow<DownloadRepository?> =
        instanceRepository.observeSelectedInstance(type)
            .map { instance ->
                instance?.let { getDownloadRepository(it.id) }
            }

    fun getRepository(instanceId: Long): InstanceScopedRepository? =
        _instanceRepositories.value[instanceId]

    fun getSelectedArrRepository(type: InstanceType): Flow<ArrInstanceRepository?> =
        instanceRepository.observeSelectedInstance(type)
            .map { instance ->
                instance?.let { getArrRepository(it.id) }
            }

    fun getSelectedSeerrRepository(): Flow<SeerrInstanceRepository?> = flow { emit(null) }
//        instanceRepository.observeSelectedInstance(InstanceType.Seerr)
//            .map { instance ->
//                instance?.let { getSeerrRepository(it.id) }
//            }

    fun getAllRepositories(): List<InstanceScopedRepository> {
        return _instanceRepositories.value.values.toList()
    }

    fun getAllArrRepositories(): List<ArrInstanceRepository> {
        return _instanceRepositories.value.values.filterIsInstance<ArrInstanceRepository>()
    }

    fun getAllSeerrRepositories(): List<SeerrInstanceRepository> {
        return _instanceRepositories.value.values.filterIsInstance<SeerrInstanceRepository>()
    }

    fun repositoriesByType(type: InstanceType): Flow<List<InstanceScopedRepository>> =
        instanceRepository.observeInstancesByType(type)
            .map { instances ->
                val current = _instanceRepositories.value
                instances.mapNotNull { current[it.id] }
            }

    fun getRepositoriesByType(type: InstanceType): List<InstanceScopedRepository> {
        return _instanceRepositories.value.values
            .filter { it.instance.type == type }
    }

    fun cleanup() {
        scope.cancel()
    }
}