package com.dnfapps.arrmatey.compose.utils

import com.dnfapps.arrmatey.instances.model.InstanceType
import com.dnfapps.arrmatey.shared.MR
import dev.icerock.moko.resources.StringResource

enum class SortBy(
    val iosIcon: String,
    val resource: StringResource
) {
    Title("textformat", MR.strings.title),
    Year("calendar", MR.strings.year),
    Added("clock.fill", MR.strings.added),
    Rating("star.fill", MR.strings.rating),
    FileSize("opticaldiscdrive.fill", MR.strings.file_size),

    // Movies
    Grabbed("arrow.down.circle.fill", MR.strings.grabbed),
    DigitalRelease("play.tv", MR.strings.digital_release),

    // TV
    NextAiring("clock", MR.strings.next_airing),
    PreviousAiring("clock.arrow.trianglehead.counterclockwise.rotate.90", MR.strings.previous_airing),

    // Lookup
    Relevance("star", MR.strings.relevance);

    companion object {

        private val sonarrOps by lazy {
            listOf(Title, Year, Added, Rating, FileSize, NextAiring, PreviousAiring)
        }
        private val radarrOps by lazy {
            listOf(Title, Year, Added, Rating, FileSize, Grabbed, DigitalRelease)
        }

        private val lidarrOps by lazy {
            listOf(Title, Year, Added, Rating, FileSize)
        }

        fun typeEntries(type: InstanceType) =
            when (type) {
                InstanceType.Sonarr -> sonarrOps
                InstanceType.Radarr -> radarrOps
                InstanceType.Lidarr -> lidarrOps
                InstanceType.Prowlarr -> emptyList()
            }

        fun lookupEntries() = listOf(Relevance, Year, Rating)
    }
}

enum class SortOrder(
    val iosIcon: String,
    val resource: StringResource
) {
    Asc("arrow.up", MR.strings.sort_ascending),
    Desc("arrow.down", MR.strings.sort_descending)
}
