package com.github.davinkevin.podcastserver.service

import lan.dk.podcastserver.service.properties.Backup
import org.rauschig.jarchivelib.ArchiveFormat
import org.rauschig.jarchivelib.ArchiverFactory
import org.rauschig.jarchivelib.CompressionType
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.ChronoField.*
import javax.persistence.EntityManager

/**
 * Created by kevin on 28/03/2016 for Podcast Server
 */
@Service
@ConditionalOnProperty("podcastserver.backup.enabled")
class DatabaseService(val backup: Backup, val em: EntityManager) {

    private val log = LoggerFactory.getLogger(this.javaClass.name)!!

    @Transactional
    fun backupWithDefault(): Path {
        log.info("Doing backup operation")
        val result = backup(this.backup.location, this.backup.binary)
        log.info("End of backup operation")
        return result
    }

    @Transactional
    @Throws(IOException::class)
    fun backup(destinationDirectory: Path, isBinary: Boolean = false): Path {

        if (!Files.isDirectory(destinationDirectory)) {
            log.error("The path {} is not a directory, can't be use for backup", destinationDirectory.toString())
            return destinationDirectory
        }

        val backupFile = destinationDirectory.toAbsolutePath().resolve("podcast-server-" + ZonedDateTime.now().format(formatter) + if (isBinary) "" else ".sql")

        // Simpler way to execute query via JPA, ExecuteUpdate not allowed here
        em.createNativeQuery(generateQuery(isBinary, backupFile)).resultList


        archiver.create(backupFile.fileName.toString(), backupFile.parent.toFile(), backupFile.toFile())
        Files.deleteIfExists(backupFile)

        return backupFile.resolveSibling("${backupFile.fileName}.tar.gz")
    }

    private fun generateQuery(isBinary: Boolean, backupFile: Path) =
            if (isBinary) "BACKUP TO '$backupFile'"
            else "SCRIPT TO '$backupFile'"

    companion object {

        private val archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP)
        private val formatter = DateTimeFormatterBuilder()
                .appendValue(YEAR, 4)
                .appendLiteral("-")
                .appendValue(MONTH_OF_YEAR, 2)
                .appendLiteral("-")
                .appendValue(DAY_OF_MONTH, 2)
                .appendLiteral("-")
                .appendValue(HOUR_OF_DAY, 2)
                .appendLiteral("-")
                .appendValue(MINUTE_OF_HOUR, 2)
                .toFormatter()
    }
}

