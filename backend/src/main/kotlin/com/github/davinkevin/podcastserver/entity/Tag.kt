package com.github.davinkevin.podcastserver.entity

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.apache.commons.lang3.builder.EqualsBuilder
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

/**
 * Created by kevin on 07/06/2014.
 */
@Entity
@JsonIgnoreProperties(ignoreUnknown = true)
class Tag {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    var id: UUID? = null

    @Column(unique = true)
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
