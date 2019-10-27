package com.github.davinkevin.podcastserver.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.EqualsBuilder
import java.util.*

/**
 * Created by kevin on 07/06/2014.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
class Tag {

    var id: UUID? = null
    var name: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Tag) return false

        return EqualsBuilder()
                .append(id, other.id)
                .append(name, other.name)
                .isEquals
    }

    override fun hashCode(): Int {
        return name!!.hashCode()
    }
}
