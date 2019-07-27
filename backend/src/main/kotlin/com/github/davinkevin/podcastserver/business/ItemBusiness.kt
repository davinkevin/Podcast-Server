package com.github.davinkevin.podcastserver.business

import com.github.davinkevin.podcastserver.manager.ItemDownloadManager
import lan.dk.podcastserver.repository.ItemRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
@Transactional
class ItemBusiness(val itemDownloadManager: ItemDownloadManager, val itemRepository: ItemRepository) {

    fun delete(id: UUID) {
        val itemToDelete = itemRepository.findById(id)
                .orElseThrow { RuntimeException("Item with ID $id not found") }

        //* Si le téléchargement est en cours ou en attente : *//
        itemDownloadManager.removeItemFromQueueAndDownload(itemToDelete)
        itemToDelete.podcast?.items!!.remove(itemToDelete)
        itemRepository.delete(itemToDelete)
    }
}
