package com.github.davinkevin.podcastserver.database

import com.github.davinkevin.podcastserver.entity.Status
import org.jooq.impl.EnumConverter

/**
 * Created by kevin on 28/12/2019
 */
class StatusConverter: EnumConverter<String, Status>(String::class.java, Status::class.java)
