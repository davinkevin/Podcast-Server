package com.github.davinkevin.podcastserver.business

import com.nhaarman.mockitokotlin2.*
import io.vavr.API.*
import lan.dk.podcastserver.entity.Tag
import lan.dk.podcastserver.repository.TagRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
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
    fun `should find all`() {
        /* Given */
        whenever(tagRepository.findAll()).thenReturn(listOf())

        /* When */
        val tags = tagBusiness.findAll()

        /* Then */
        assertThat(tags.toJavaList()).isEqualTo(listOf<Tag>())
        verify(tagRepository, times(1)).findAll()
    }

    @Test
    fun `should find one`() {
        /* Given */
        val tagId = UUID.randomUUID()
        val tag = Tag().apply { id = tagId }
        whenever(tagRepository.findById(any())).thenReturn(Optional.of(tag))

        /* When */
        val tagToFind = tagBusiness.findOne(tagId)

        /* Then */
        assertThat(tagToFind).isSameAs(tag)
        verify(tagRepository, times(1)).findById(eq(tagId))
    }

    @Test
    fun `should throw exception if tags not found`() {
        /* Given */
        val id = UUID.randomUUID()
        whenever(tagRepository.findById(any())).thenReturn(Optional.empty())

        /* When */
        assertThatThrownBy { tagBusiness.findOne(id) }

        /* Then */
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("Tag with ID $id not found")
        verify(tagRepository, times(1)).findById(eq(id))
    }

    @Test
    fun `should find by name like`() {
        /* Given */
        val searchWord = "name"
        val foundTags = Set<Tag>()
        whenever(tagRepository.findByNameContainsIgnoreCase(anyString())).thenReturn(foundTags)

        /* When */
        val tags = tagBusiness.findByNameLike(searchWord)

        /* Then */
        assertThat(tags).isSameAs(foundTags)
        verify(tagRepository, times(1)).findByNameContainsIgnoreCase(eq(searchWord))
    }

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
        doAnswer { (it.getArgument(0) as Tag).setId(UUID.randomUUID()) }.whenever(tagRepository).save<Tag>(any())

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
