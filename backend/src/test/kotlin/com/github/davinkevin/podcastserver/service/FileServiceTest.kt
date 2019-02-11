package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.service.properties.PodcastServerParameters
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.util.FileSystemUtils
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Created by kevin on 2019-02-12
 */
@ExtendWith(SpringExtension::class)
@Import(FileService::class)
class FileServiceTest {

    @Autowired lateinit var fileService: FileService
    @MockBean lateinit var p: PodcastServerParameters

    private val tempFolder = Paths.get("/tmp", "podcast-server", "FileService")

    @BeforeEach
    fun beforeEach() {
        Files.createDirectories(tempFolder)
    }

    @AfterEach
    fun afterEach() {
        FileSystemUtils.deleteRecursively(tempFolder.toFile())
    }

    @Test
    fun `should delete file relatively`() {
        /* Given */
        whenever(p.rootfolder).thenReturn(tempFolder)
        val file = Paths.get("foo.txt")
        val f = Files.createFile(tempFolder.resolve(file))

        /* When */
        fileService.deleteItem(file)

        /* Then */
        assertThat(Files.exists(f)).isFalse()
    }
}