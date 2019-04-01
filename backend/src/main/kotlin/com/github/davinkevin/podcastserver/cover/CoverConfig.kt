package com.github.davinkevin.podcastserver.cover

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(CoverRepositoryV2::class)
class CoverConfig
