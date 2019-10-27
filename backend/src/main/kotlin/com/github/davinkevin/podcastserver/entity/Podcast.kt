package com.github.davinkevin.podcastserver.entity


import arrow.core.toOption
import com.fasterxml.jackson.annotation.*
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.control.Option
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.io.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZonedDateTime
import java.util.*

@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = ["signature", "items", "contains", "add", "lastUpdateToNow"])
open class Podcast : Serializable {

    var id: UUID? = null
    var title: String? = null
    var url: String? = null
    var signature: String? = null
    var type: String? = null
    var lastUpdate: ZonedDateTime? = null
    @JsonIgnore
    var cover: Cover? = null
    var description: String? = null
    var hasToBeDeleted: Boolean? = null
    val coverPath: Option<Path>
        @JsonIgnore
        get() = arrow.core.Option.fromNullable(cover)
                .flatMap { it.url.toOption() }
                .map { FilenameUtils.getExtension(it) }
                .map { ext -> rootFolder!!.resolve(title).resolve("cover.$ext") }
                .toVΛVΓ()

    var coverOfPodcast: Cover
        @JsonProperty("cover")
        get() {
            return coverPath
                    .filter { Files.exists(it) }
                    .filter { this.id != null }
                    .flatMap { arrow.core.Option.fromNullable(cover)
                            .map { c -> String.format(COVER_PROXY_URL, id, FilenameUtils.getExtension(c.url)) }
                            .map { anUrl -> val c = this.cover!!
                                return@map Cover().apply {
                                    url = anUrl
                                    height = c.height
                                    width = c.width
                                    id = c.id
                                }
                            }
                            .toVΛVΓ()
                    }
                    .getOrElse { cover!! }
        }
        set(value) {
            cover = value
        }

    @java.beans.ConstructorProperties("id", "title", "url", "signature", "type", "lastUpdate", "items", "cover", "description", "hasToBeDeleted", "tags")
    @JsonIgnore
    constructor(id: UUID, title: String, url: String, signature: String, type: String, lastUpdate: ZonedDateTime, cover: Cover, description: String, hasToBeDeleted: Boolean?) {
        this.id = id
        this.title = title
        this.url = url
        this.signature = signature
        this.type = type
        this.lastUpdate = lastUpdate
        this.cover = cover
        this.description = description
        this.hasToBeDeleted = hasToBeDeleted
    }

    constructor()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Podcast) return false

        return EqualsBuilder()
                .append(id, other.id)
                .append(title, other.title)
                .append(url, other.url)
                .append(signature, other.signature)
                .append(lastUpdate, other.lastUpdate)
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder(17, 37)
                .append(id)
                .append(title)
                .append(url)
                .append(signature)
                .append(lastUpdate)
                .toHashCode()
    }

    companion object {
        var rootFolder: Path? = null
        val DEFAULT_PODCAST = Podcast()
        const val COVER_PROXY_URL = "/api/v1/podcasts/%s/cover.%s"
    }
}
