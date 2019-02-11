package com.github.davinkevin.podcastserver.item

import java.nio.file.Paths
import java.util.*

/**
 * Created by kevin on 2019-02-09
 */
class DeleteItemInformation(val id: UUID, fileName: String, podcastTitle: String) {
    val path = Paths.get(podcastTitle, fileName)!!
}