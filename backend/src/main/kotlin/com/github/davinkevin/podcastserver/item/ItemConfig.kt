package com.github.davinkevin.podcastserver.item

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Created by kevin on 2019-02-03
 */
@Configuration
@Import(ItemRepositoryV2::class)
class ItemConfig