package lan.dk.podcastserver.service;

import lan.dk.podcastserver.service.properties.Backup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import javax.persistence.Query;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.nonNull;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 29/03/2016 for Podcast Server
 */
@Slf4j
@RunWith(MockitoJUnitRunner.class)
public class DatabaseServiceTest {

    @Mock Backup backup;
    @Mock FullTextEntityManager fem;
    @InjectMocks
    DatabaseService databaseService;
    private static final Path NOT_DIRECTORY = Paths.get("/tmp", "foo.bar");
    private Query query;
    private Path backupToCreate;

    @Before
    public void beforeEach() throws IOException {
       Files.deleteIfExists(NOT_DIRECTORY);
       if (nonNull(backupToCreate)) {
            Files.deleteIfExists(backupToCreate);
        }
       //FileSystemUtils.deleteRecursively(Paths.get("/tmp", "foo.bar").toFile());
    }

    @Test
    public void should_reject_if_destination_isnt_directory() throws IOException {
        /* Given */
        Files.createFile(NOT_DIRECTORY);

        /* When */
        Path backupFile = databaseService.backup(NOT_DIRECTORY, true);

        /* Then */
        assertThat(backupFile).isSameAs(NOT_DIRECTORY);
        verify(fem, never()).createNativeQuery(anyString());
    }

    @Test
    public void should_generate_an_archive_of_db_in_binary() throws IOException {
        /* Given */
        when(fem.createNativeQuery(anyString())).then(generateDumpFile());

        /* When */
        Path backupFile = databaseService.backup(Paths.get("/tmp"), true);

        /* Then */
        verify(fem, times(1)).createNativeQuery(contains("BACKUP TO"));
        assertThat(backupFile).exists().hasFileName(backupToCreate.getFileName() + ".tar.gz");
    }

    @Test
    public void should_generate_an_archive_of_db_in_sql() throws IOException {
        /* Given */
        when(fem.createNativeQuery(anyString())).then(generateDumpFile());

        /* When */
        Path backupFile = databaseService.backup(Paths.get("/tmp"), false);

        /* Then */
        verify(fem, times(1)).createNativeQuery(contains("SCRIPT TO"));
        assertThat(backupFile)
                .exists()
                .hasFileName(backupToCreate.getFileName() + ".tar.gz");
    }

    @Test
    public void should_generate_from_backup_parameters() throws IOException {
        /* Given */
        when(fem.createNativeQuery(anyString())).then(generateDumpFile());
        when(backup.getBinary()).thenReturn(Boolean.FALSE);
        when(backup.getLocation()).thenReturn(Paths.get("/tmp"));

        /* When */
        Path backupFile = databaseService.backupWithDefault();

        /* Then */
        verify(fem, times(1)).createNativeQuery(contains("SCRIPT TO"));
        assertThat(backupFile)
                .exists()
                .hasFileName(backupToCreate.getFileName() + ".tar.gz");
    }

    private Answer<Object> generateDumpFile() {
        return i -> {
            backupToCreate = Paths.get(StringUtils.substringAfter(i.getArguments()[0].toString(), "\'").replace("'", ""));
            Files.createFile(backupToCreate);
            query = mock(Query.class);
            return query;
        };
    }
}