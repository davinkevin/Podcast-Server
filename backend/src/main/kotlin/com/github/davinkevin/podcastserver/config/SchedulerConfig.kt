package com.github.davinkevin.podcastserver.config

import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

@Configuration
@EnableScheduling
@ComponentScan(basePackages = arrayOf("com.github.davinkevin.podcastserver.scheduled"))
class SchedulerConfig
