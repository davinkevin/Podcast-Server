package com.github.davinkevin.podcastserver.service

import com.github.davinkevin.podcastserver.service.properties.Backup
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.contains
import org.mockito.ArgumentMatchers.startsWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoExtension
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.persistence.EntityManager
import javax.persistence.Query

/**
 * Created by kevin on 29/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class DatabaseServiceTest {

    @Mock lateinit var backup: Backup
    @Mock lateinit var em: EntityManager
    @InjectMocks lateinit var databaseService: DatabaseService

    private var query: Query? = null
    private var backupToCreate: Path? = null

    @Nested
    inner class ErrorCases {

        @BeforeEach @AfterEach
        fun beforeEach() {
            Files.deleteIfExists(NOT_DIRECTORY)
        }

        @Test
        fun `should reject if destination is not a directory`() {
            /* Given */
            Files.createFile(NOT_DIRECTORY)

            /* When */
            val backupFile = databaseService.backup(NOT_DIRECTORY, true)

            /* Then */
            assertThat(backupFile).isSameAs(NOT_DIRECTORY)
            verify<EntityManager>(em, never()).createNativeQuery(anyString())
        }
    }

    @Nested
    inner class BackupGeneration {
        @AfterEach
        fun afterEach() {
            Files.deleteIfExists(backupToCreate)
        }

        @Test
        fun `should generate an archive of db in binary format`() {
            /* Given */
            whenever(em.createNativeQuery(anyString())).then { generateDumpFile(it) }

            /* When */
            val backupFile = databaseService.backup(Paths.get("/tmp"), true)

            /* Then */
            verify(em, times(1)).createNativeQuery(contains("BACKUP TO"))
            assertThat(backupFile).exists().hasFileName("${backupToCreate!!.fileName}.tar.gz")
        }

        @Test
        fun `should generate an archive of db in sql format`() {
            /* Given */
            whenever(em.createNativeQuery(startsWith("SCRIPT TO"))).then { generateDumpFile(it) }

            /* When */
            val backupFile = databaseService.backup(Paths.get("/tmp"), false)

            /* Then */
            verify(em, times(1)).createNativeQuery(contains("SCRIPT TO"))
            assertThat(backupFile)
                    .exists()
                    .hasFileName("${backupToCreate!!.fileName}.tar.gz")
        }

        @Test
        fun `should generate from backup parameters`() {
            /* Given */
            whenever(backup.binary).thenReturn(false)
            whenever(backup.location).thenReturn(Paths.get("/tmp"))
            whenever(em.createNativeQuery(startsWith("SCRIPT TO"))).then { generateDumpFile(it) }

            /* When */
            val backupFile = databaseService.backupWithDefault()

            /* Then */
            verify(em, times(1)).createNativeQuery(contains("SCRIPT TO"))
            assertThat(backupFile)
                    .exists()
                    .hasFileName("${backupToCreate!!.fileName}.tar.gz")
        }

        private fun generateDumpFile(i: InvocationOnMock): Query? {
            backupToCreate = Paths.get(i.arguments[0].toString().substringAfter("\'").replace("'", ""))
            Files.createFile(backupToCreate)
            query = mock()
            return query
        }
    }

    companion object {
        private val NOT_DIRECTORY = Paths.get("/tmp", "foo.bar")
    }
}
