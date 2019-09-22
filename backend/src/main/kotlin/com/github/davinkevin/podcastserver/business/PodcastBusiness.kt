package com.github.davinkevin.podcastserver.business

import com.github.davinkevin.podcastserver.entity.Podcast
import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import lan.dk.podcastserver.repository.PodcastRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Component
@Transactional
class PodcastBusiness(val podcastRepository: PodcastRepository) {
    fun delete(id: UUID) = podcastRepository.deleteById(id)
}
