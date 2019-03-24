package com.github.davinkevin.podcastserver.business

import com.github.davinkevin.podcastserver.entity.Tag
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argWhere
import com.nhaarman.mockitokotlin2.doAnswer
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.whenever
import io.vavr.API.None
import io.vavr.API.Option
import io.vavr.API.Set
import lan.dk.podcastserver.repository.TagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import java.util.*

/**
 * Created by kevin on 23/07/15 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class TagBusinessTest {

    @Mock lateinit var tagRepository: TagRepository
    @InjectMocks lateinit var tagBusiness: TagBusiness

    @Test
    fun `should get tag by name in set`() {
        /* Given */
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val tag1 = Tag().apply { id = id1; name = "tag$id1" }
        val tag2 = Tag().apply { id = id2; name = "tag$id2" }
        val tag3 = Tag().apply { name = "Foo" }
        val tag4 = Tag().apply { name = "Bar" }

        doReturn(Option(tag1)).whenever(tagRepository).findByNameIgnoreCase(eq(tag1.name))
        doReturn(None<Tag>()).whenever(tagRepository).findByNameIgnoreCase(argWhere { it != tag1.name})
        doAnswer {
            val t = it.getArgument<Tag>(0)
            t.id = UUID.randomUUID()
            t
        }.whenever(tagRepository).save<Tag>(any())

        /* When */
        val tagListByName = tagBusiness.getTagListByName(setOf(tag1, tag2, tag3, tag4))

        /* Then */
        assertThat(tagListByName)
                .extracting("name", String::class.java)
                .contains(tag1.name, tag2.name, tag3.name, tag4.name)
    }

    @Test
    fun `should find all by names`() {
        /* GIVEN */
        val tag1 = Tag().apply { id = UUID.randomUUID(); name = "Super Foo" }
        val tag2 = Tag().apply { id = UUID.randomUUID(); name = "BarOu !" }
        doReturn(Option(tag1)).whenever(tagRepository).findByNameIgnoreCase("foo")
        doReturn(Option(tag2)).whenever(tagRepository).findByNameIgnoreCase("bar")

        /* WHEN  */
        val tags = tagBusiness.findAllByName(Set("foo", "bar"))

        /* THEN  */
        assertThat(tags).contains(tag1, tag2)
    }
}
