package com.github.davinkevin.podcastserver.entity

import org.apache.commons.lang3.builder.EqualsBuilder
import org.apache.commons.lang3.builder.HashCodeBuilder
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
open class Cover {

    @Id
    @GeneratedValue
    @Column(columnDefinition = "UUID")
    var id: UUID? = null
    var url: String? = null
    var width: Int? = null
    var height: Int? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Cover) return false

        return EqualsBuilder()
                .append(url?.toLowerCase(), other.url?.toLowerCase())
                .isEquals
    }

    override fun hashCode(): Int {
        return HashCodeBuilder(17, 37)
                .append(url?.toLowerCase())
                .toHashCode()
    }

    companion object {
        val DEFAULT_COVER = Cover()
    }
}
