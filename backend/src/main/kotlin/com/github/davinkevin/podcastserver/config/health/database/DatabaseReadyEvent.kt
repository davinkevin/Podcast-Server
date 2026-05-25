package com.github.davinkevin.podcastserver.config.health.database

import org.springframework.context.ApplicationEvent

class DatabaseReadyEvent(source: Any) : ApplicationEvent(source)
