package com.github.davinkevin.podcastserver.find.finders.noop

import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

/**
 * Created by kevin on 02/11/2019
 */
@Configuration
@Import(NoOpFinder::class)
class NoopConfig
