package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.database.Tables.TAG
import org.jooq.DSLContext
import java.util.*

class TagRepository(val query: DSLContext) {

    fun findById(id: UUID): Tag?  {
        val (name) = query
            .select(TAG.NAME)
            .from(TAG)
            .where(TAG.ID.eq(id))
            .fetchOne()
            ?: return null

        return Tag(id, name)
    }

    fun findByNameLike(name: String): List<Tag> {
        return query
            .select(TAG.ID, TAG.NAME)
            .from(TAG)
            .where(TAG.NAME.containsIgnoreCase(name))
            .orderBy(TAG.NAME.asc())
            .fetch()
            .map { (id, name) -> Tag(id, name) }
    }

    fun save(name: String): Tag {
        val (id) = query
            .select(TAG.ID, TAG.NAME)
            .from(TAG)
            .where(TAG.NAME.eq(name))
            .fetchOne()
            ?: return create(name)

        return Tag(id, name)
    }

    private fun create(name: String): Tag {
        val id = UUID.randomUUID()
        query
            .insertInto(TAG)
            .set(TAG.ID, id)
            .set(TAG.NAME, name)
            .execute()

        return Tag(id, name)
    }

}
