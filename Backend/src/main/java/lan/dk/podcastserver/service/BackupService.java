package lan.dk.podcastserver.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.rauschig.jarchivelib.ArchiveFormat;
import org.rauschig.jarchivelib.Archiver;
import org.rauschig.jarchivelib.ArchiverFactory;
import org.rauschig.jarchivelib.CompressionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

import static java.time.temporal.ChronoField.*;

/**
 * Created by kevin on 28/03/2016 for Podcast Server
 */
@Slf4j
@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class BackupService {

    private static final Archiver archiver = ArchiverFactory.createArchiver(ArchiveFormat.TAR, CompressionType.GZIP);

    private static final DateTimeFormatter formatter = new DateTimeFormatterBuilder()
            .appendValue(YEAR, 4)
            .appendLiteral("-")
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral("-")
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral("-")
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral("-")
            .appendValue(MINUTE_OF_HOUR, 2)
            .toFormatter();

    private static final String QUERY_BACKUP_SQL = "SCRIPT TO '%s'";
    private static final String QUERY_BACKUP_BINARY = "BACKUP TO '%s'";

    final FullTextEntityManager em;

    @Transactional
    public Path backup(Path destinationDirectory, Boolean isBinary) throws IOException {

        if (!Files.isDirectory(destinationDirectory)) {
            log.error("The path {} is not a directory, can't be use for backup", destinationDirectory.toString());
            return destinationDirectory;
        }

        Path backupFile = destinationDirectory.toAbsolutePath().resolve("podcast-server-" + ZonedDateTime.now().format(formatter) + (isBinary ? "" : ".sql"));

        em.createNativeQuery(
                String.format(isBinary ? QUERY_BACKUP_BINARY : QUERY_BACKUP_SQL,backupFile.toString())
        ).getResultList(); // Simpler way to execute query via JPA, ExecuteUpdate not allowed here


        archiver.create(backupFile.getFileName().toString(), backupFile.getParent().toFile(), backupFile.toFile());
        Files.deleteIfExists(backupFile);

        return backupFile.resolveSibling(backupFile.getFileName() + ".tar.gz");
    }

}

