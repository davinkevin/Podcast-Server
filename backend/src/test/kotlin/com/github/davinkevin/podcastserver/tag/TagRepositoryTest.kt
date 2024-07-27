package com.github.davinkevin.podcastserver.tag

import com.github.davinkevin.podcastserver.database.tables.Tag.TAG
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.jooq.impl.DSL.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import java.util.UUID.fromString

/**
 * Created by kevin on 2019-03-24
 */
@JooqTest
@Import(TagRepository::class)
class TagRepositoryTest(
    @Autowired val repository: TagRepository,
    @Autowired val query: DSLContext
) {

    @BeforeEach
    fun beforeEach() {
        query.batch(
            truncate(TAG).cascade(),
            insertInto(TAG)
                .columns(TAG.ID, TAG.NAME)
                .values(fromString("eb355a23-e030-4966-b75a-b70881a8bd08"), "Foo")
                .values(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "bAr")
                .values(fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"), "Another Bar")
        )

            .execute()
    }

    @Nested
    @DisplayName("Should find by id")
    inner class ShouldFindById {

        @Test
        fun `and return one matching element`() {
            /* Given */
            val id = fromString("eb355a23-e030-4966-b75a-b70881a8bd08")

            /* When */
            val tag = repository.findById(id)

            /* Then */
            assertThat(tag).isEqualTo(Tag(id, "Foo"))
        }

        @Test
        fun `and return empty mono if not find by id`() {
            /* Given */
            val id = fromString("98b33370-a976-4e4d-9ab8-57d47241e693")

            /* When */
            val tag = repository.findById(id)

            /* Then */
            assertThat(tag).isNull()
        }

    }

    @Nested
    @DisplayName("Should find by name")
    inner class ShouldFindByNameLike {

        @Test
        fun `with with insensitive case results`() {
            /* Given */
            /* When */
            val tags = repository.findByNameLike("bar")

            /* Then */
            assertThat(tags).containsExactly(
                Tag(fromString("ad109389-9568-4bdb-ae61-6f26bf6ffdf6"), "Another Bar"),
                Tag(fromString("ad109389-9568-4bdb-ae61-5f26bf6ffdf6"), "bAr")
            )
        }

        @Test
        fun `without any match`() {
            /* Given */
            /* When */
            val tags = repository.findByNameLike("boo")
            /* Then */
            assertThat(tags).isEmpty()
        }
    }

    @Nested
    @DisplayName("Should save ")
    inner class ShouldSave {

        @Test
        fun `an item with just name`() {
            /* Given */
            val name = "a_wonderful_tag_name"

            /* When */
            val tag = repository.save(name)

            /* Then */
            assertThat(tag.id).isNotNull()
            assertThat(tag.name).isEqualTo(name)

            val numberOfTag = query.selectCount().from(TAG).fetchOne(count())
            assertThat(numberOfTag).isEqualTo(4)
        }

        @Test
        fun `an item already existing`() {
            /* Given */
            val name = "Foo"
            /* When */
            val tag = repository.save(name)

            /* Then */
            assertThat(tag.id).isNotNull()
            assertThat(tag.name).isEqualTo(name)

            val numberOfTag = query.selectCount().from(TAG).fetchOne(count())
            assertThat(numberOfTag).isEqualTo(3)

        }

    }
}
