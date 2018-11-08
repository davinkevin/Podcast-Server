package com.github.davinkevin.podcastserver.business

import arrow.core.Option
import arrow.core.getOrElse
import com.github.davinkevin.podcastserver.service.MimeTypeService
import com.github.davinkevin.podcastserver.utils.toVΛVΓ
import io.vavr.collection.Set
import lan.dk.podcastserver.entity.Item
import lan.dk.podcastserver.entity.Status
import lan.dk.podcastserver.entity.Tag
import lan.dk.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.repository.ItemRepository
import lan.dk.podcastserver.repository.dsl.ItemDSL.getSearchSpecifications
import lan.dk.podcastserver.service.properties.PodcastServerParameters
import org.apache.commons.io.FilenameUtils
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Isolation
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.nio.file.Files
import java.time.*
import java.time.ZonedDateTime.now
import java.time.ZonedDateTime.of
import java.time.format.DateTimeFormatter
import java.util.*

@Component
@Transactional
class ItemBusiness(val itemDownloadManager: ItemDownloadManager, val parameters: PodcastServerParameters, val itemRepository: ItemRepository, val podcastBusiness: PodcastBusiness, val mimeTypeService: MimeTypeService) {

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    fun findAll(pageable: Pageable): Page<Item> = itemRepository.findAll(pageable)

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    fun findByTagsAndFullTextTerm(term: String, tags: Set<Tag>, statuses: Set<Status>, page: Pageable): Page<Item> {
        return Option.just(page.sort)
                .flatMap { Option.fromNullable(it.getOrderFor("pertinence")) }
                .map { findByTagsAndFullTextTermOrderByPertinence(term, tags, statuses, page) }
                .getOrElse { itemRepository.findAll(getSearchSpecifications(if (term.isEmpty()) null else itemRepository.fullTextSearch(term), tags, statuses), page) }
    }

    private fun findByTagsAndFullTextTermOrderByPertinence(term: String?, tags: Set<Tag>, state: Set<Status>, page: Pageable): Page<Item> {
        val idDesc = itemRepository.fullTextSearch(term).toJavaList()!!

        val direction = page.sort.getOrderFor("pertinence")!!.direction

        val idsOrdered = if (direction == Sort.Direction.ASC) idDesc.reversed() else idDesc

        val allResult = itemRepository.findAll(getSearchSpecifications(idsOrdered.toVΛVΓ(), tags, state))

        val v = idsOrdered
                .map { id -> allResult.find { item -> id == item.id } }
                .filter { it != null }
                .drop(page.offset.toInt())
                .take(page.pageSize)

        return PageImpl(v, page, v.size.toLong())
    }

    fun save(entity: Item): Item = itemRepository.save(entity)

    @Transactional(readOnly = true, isolation = Isolation.READ_COMMITTED)
    fun findOne(id: UUID): Item =
            itemRepository.findById(id)
                    .orElseThrow { RuntimeException("Item with ID $id not found") }

    fun delete(id: UUID) {
        val itemToDelete = findOne(id)

        //* Si le téléchargement est en cours ou en attente : *//
        itemDownloadManager.removeItemFromQueueAndDownload(itemToDelete)
        itemToDelete.podcast.items.remove(itemToDelete)
        itemRepository.delete(itemToDelete)
    }

    @Transactional(readOnly = true)
    fun findByPodcast(idPodcast: UUID, pageable: Pageable): Page<Item> =
            itemRepository.findByPodcast(idPodcast, pageable)

    @Throws(InterruptedException::class)
    fun reindex() = itemRepository.reindex()

    fun reset(id: UUID): Item? {
        val itemToReset = findOne(id)

        return when {
            itemDownloadManager.isInDownloadingQueue(itemToReset)!! -> null
            else -> save(itemToReset.reset())
        }

    }

    fun addItemByUpload(podcastId: UUID, uploadedFile: MultipartFile): Item {
        val p = podcastBusiness.findOne(podcastId)

        //TODO utiliser BEAN_UTIL pour faire du dynamique :
        // 1er temps : Template en dure : {podcast-title} - {date} - {title}.mp3

        val originalFilename = uploadedFile.originalFilename

        val fileToSave = parameters.rootfolder.resolve(p.title).resolve(originalFilename)
        Files.deleteIfExists(fileToSave)
        Files.createDirectories(fileToSave.parent)

        uploadedFile.transferTo(fileToSave.toFile())

        var item = Item().apply {
            title = FilenameUtils.removeExtension(originalFilename!!.split(" - ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[2])
            pubDate = fromFileName(originalFilename.split(" - ".toRegex()).dropLastWhile { it.isEmpty() }[1])
            length = uploadedFile.size
            mimeType = mimeTypeService.getMimeType(FilenameUtils.getExtension(originalFilename))
            description = p.description
            fileName = originalFilename
            downloadDate = of(LocalDateTime.now(), ZoneId.systemDefault())
            podcast = p
            status = Status.FINISH

        }

        p.apply {
            items.add(item)
            lastUpdate = now()
        }

        item = save(item)
        podcastBusiness.save(p)

        return item
    }

    private fun fromFileName(pubDate: String): ZonedDateTime {
        return of(LocalDateTime.of(LocalDate.parse(pubDate, DateTimeFormatter.ofPattern(UPLOAD_PATTERN)), LocalTime.of(0, 0)), ZoneId.systemDefault())
    }

    companion object {
        private const val UPLOAD_PATTERN = "yyyy-MM-dd"
    }

}
