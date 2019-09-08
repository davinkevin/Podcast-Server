package com.github.davinkevin.podcastserver.entity

import arrow.core.getOrElse
import arrow.core.toOption
import com.fasterxml.jackson.annotation.*
import com.github.davinkevin.podcastserver.manager.worker.upload.UploadUpdater
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.API.Option
import io.vavr.API.Try
import io.vavr.control.Option
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import org.springframework.web.util.UriComponentsBuilder
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZonedDateTime
import java.util.*
import java.util.Objects.nonNull
import javax.persistence.*
import javax.validation.constraints.AssertTrue
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

@Entity
@Table(name = "item", uniqueConstraints = [UniqueConstraint(columnNames = arrayOf("podcast_id", "url"))])
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
@JsonIgnoreProperties(ignoreUnknown = true, value = ["numberOfTry", "localUri", "addATry", "deleteDownloadedFile", "localPath", "proxyURLWithoutExtention", "extention", "hasValidURL", "reset", "coverPath"])
@EntityListeners(AuditingEntityListener::class)
class Item {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    var id: UUID? = null

    @OneToOne(fetch = FetchType.EAGER, cascade = [CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.DETACH])
    var cover: Cover? = null

    @ManyToOne(cascade = [CascadeType.MERGE], fetch = FetchType.EAGER)
    @JsonBackReference("podcast-item")
    var podcast: Podcast? = null

    @NotNull
    @JsonView(ItemSearchListView::class)
    @get:Size(min = 1, max = 254)
    var title: String? = null

    @Column(length = 65535)
    @JsonView(ItemSearchListView::class)
    var url: String? = null

    @JsonView(ItemPodcastListView::class)
    var pubDate: ZonedDateTime? = null

    @Column(length = 2147483647)
    @JsonView(ItemPodcastListView::class)
    var description: String? = null

    @JsonView(ItemSearchListView::class)
    var mimeType: String? = null

    @JsonView(ItemDetailsView::class)
    var length: Long? = null

    @JsonView(ItemDetailsView::class)
    var fileName: String? = null

    /* Value for the Download */
    @Enumerated(EnumType.STRING)
    @JsonView(ItemSearchListView::class)
    var status = Status.NOT_DOWNLOADED

    @Transient
    @JsonView(ItemDetailsView::class)
    var progression: Int? = 0

    @JsonIgnore
    var numberOfFail: Int? = 0

    @JsonView(ItemDetailsView::class)
    var downloadDate: ZonedDateTime? = null

    @CreatedDate
    var creationDate: ZonedDateTime? = null

    @JsonIgnore
    @ManyToMany(mappedBy = "items", cascade = [CascadeType.REFRESH])
    var watchLists: MutableSet<WatchList> = mutableSetOf()

    var localUri: String?
        get() = if (fileName == null) null else localPath.toString()
        set(value) { fileName = FilenameUtils.getName(value) }

    // /api/podcasts/%s/items/%s/%s%s
    val proxyURL: String
        @Transient
        @JsonProperty("proxyURL")
        @JsonView(ItemSearchListView::class)
        get() = this.proxyURLWithoutExtention + extension

    val proxyURLWithoutExtention: String
        get() = UriComponentsBuilder.fromPath("/")
                .pathSegment(
                        "api",
                        "v1",
                        "podcasts",
                        podcast!!.id.toString(),
                        "items",
                        id.toString(),
                        Option<String>(title).map { s -> s.replace("[^a-zA-Z0-9.-]".toRegex(), "_") }.getOrElse("null")
                )
                .build(true)
                .toString()

    val isDownloaded: Boolean
        @Transient
        @JsonProperty("isDownloaded")
        @JsonView(ItemSearchListView::class)
        get() = StringUtils.isNotEmpty(fileName)

    val localPath: Path
        get() = podcastPath.resolve(fileName)

    val coverPath: Option<Path>
        get() = arrow.core.Option.fromNullable(cover)
                .flatMap { it.url.toOption() }
                .filter { !StringUtils.isEmpty(it)}
                .map { it.extension() }
                .map { podcastPath.resolve("${id.toString()}.$it") }
                .toVΛVΓ()
                .orElse { podcast!!.coverPath }

    private val podcastPath: Path
        get() = rootFolder!!.resolve(podcast!!.title)


    private val extension: String
        get() = Option<String>(fileName)
                .map { FilenameUtils.getExtension(it) }
                .map { ".$it" }
                .getOrElse("")

    val coverOfItemOrPodcast: Cover
        @JsonProperty("cover")
        @JsonView(ItemSearchListView::class)
        get() = Option<Cover>(cover)
                .map { c -> String.format(COVER_PROXY_URL, podcast!!.id, id, c.url!!.extension()) }
                .map { url ->
                    val c = Cover()
                    c.url = url
                    c.height = this.cover!!.height
                    c.width = this.cover!!.width
                    c.id = this.cover!!.id
                    c
                }
                .getOrElse { podcast!!.cover }

    val podcastId: UUID?
        @JsonProperty("podcastId")
        @JsonView(ItemSearchListView::class)
        get() = arrow.core.Option.fromNullable(podcast)
                .map { it.id }
                .getOrElse { null }

    fun addATry() {
        this.numberOfFail = (this.numberOfFail ?: 0) +1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Item) return false
        if (this === DEFAULT_ITEM || other === DEFAULT_ITEM) return false

        if (nonNull(id) && nonNull(other.id))
            return id == other.id

        return if (nonNull(url) && nonNull(other.url)) url == other.url
        else StringUtils.equals(proxyURL, other.proxyURL)

    }

    override fun hashCode(): Int {
        return HashCodeBuilder(17, 37)
                .append(url)
                .toHashCode()
    }

    override fun toString(): String {
        return "Item{" +
                "id=" + id +
                ", title='" + title + '\''.toString() +
                ", url='" + url + '\''.toString() +
                ", pubDate=" + pubDate +
                ", description='" + description + '\''.toString() +
                ", mimeType='" + mimeType + '\''.toString() +
                ", length=" + length +
                ", status='" + status + '\''.toString() +
                ", progression=" + progression +
                ", downloaddate=" + downloadDate +
                ", podcast=" + podcast +
                ", numberOfTry=" + numberOfFail +
                '}'.toString()
    }

    //* CallBack Method JPA *//
    @PreRemove
    fun preRemove() {
        checkAndDelete()
        watchLists.forEach { watchList -> watchList.remove(this) }
    }

    private fun checkAndDelete() {

        if (podcast!!.hasToBeDeleted === java.lang.Boolean.FALSE) {
            return
        }

        if (Objects.nonNull(this.cover) && this.cover != podcast!!.cover) {
            coverPath.toTry()
                    .mapTry { Files.deleteIfExists(it) }
                    .onFailure { e -> log.error("Error during deletion of cover of {}", this, e) }
        }

        if (isDownloaded) {
            deleteFile()
        }
    }

    private fun deleteFile() {
        Try { Files.deleteIfExists(localPath) }
                .onFailure { e -> log.error("Error during deletion of {}", this, e) }
    }

    @Transient
    @JsonIgnore
    fun deleteDownloadedFile(): Item {
        deleteFile()
        status = Status.DELETED
        fileName = null
        return this
    }

    @AssertTrue
    fun hasValidURL(): Boolean {
        return !this.url.isNullOrEmpty() || UploadUpdater.TYPE.key == podcast!!.type
    }

    interface ItemSearchListView
    interface ItemPodcastListView : ItemSearchListView
    interface ItemDetailsView : ItemPodcastListView

    companion object {

        private val log = org.slf4j.LoggerFactory.getLogger(Item::class.java)
        var rootFolder: Path? = null
        val DEFAULT_ITEM = Item()
        private const val COVER_PROXY_URL = "/api/v1/podcasts/%s/items/%s/cover.%s"
    }
}

private fun String.extension(): String {
    val extension = FilenameUtils.getExtension(this)
    return when {
        extension.isNullOrBlank() -> "jpg"
        else -> extension
    }
}
