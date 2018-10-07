package com.github.davinkevin.podcastserver.manager.worker.mycanal

import arrow.core.Option
import arrow.core.orElse
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDateTime.parse
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.ofPattern

private const val MYCANAL_DATE_PATTERN = "dd/MM/yyyy-HH:mm:ss"

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalPageItem(val displayName: String, val pathname: String)

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalItem(val contentID: String = "", val image: String = "", val onClick: MyCanalItemOnClick)

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalItemOnClick(val displayName: String, val path: String)

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalDetailsItem(
        @JsonProperty("ID") val id: String,
        @JsonProperty("DURATION") val duration: Long,
        @JsonProperty("INFOS") val infos: MyCanalInfosItem,
        @JsonProperty("MEDIA") val media: MyCanalMediaItem,
        @JsonProperty("URL") val url: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalInfosItem(
        @JsonProperty("DESCRIPTION") val description: String,
        @JsonProperty("PUBLICATION") val publication: MyCanalPublicationItem,
        @JsonProperty("TITRAGE") val titrage: MyCanalTitrageItem
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalPublicationItem(
        @JsonProperty("DATE") val date: String,
        @JsonProperty("HEURE") val heure: String
) {
    fun asZonedDateTime() =
            ZonedDateTime.of(parse("$date-$heure", ofPattern(MYCANAL_DATE_PATTERN)), ZoneId.of("Europe/Paris"))!!
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalTitrageItem(
        @JsonProperty("TITRE") val titre: String,
        @JsonProperty("SOUS_TITRE") val sous_titre: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalMediaItem(@JsonProperty("IMAGES") val images: MyCanalImageItem)

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalImageItem(
        @JsonProperty("GRAND") val grand: String,
        @JsonProperty("PETIT") val petit: String
) {
    fun cover() = Option.fromNullable(grand).orElse { Option.fromNullable(petit) }
}

@JsonIgnoreProperties(ignoreUnknown = true)
internal class MyCanalVideoItem(
        @JsonProperty("BAS_DEBIT") val bas_debit: String,
        @JsonProperty("HAUT_DEBIT") val haut_debit: String,
        @JsonProperty("HD") val hd: String,
        @JsonProperty("MOBILE") val mobile: String,
        @JsonProperty("HDS") val hds: String,
        @JsonProperty("HLS") val hls: String
)
