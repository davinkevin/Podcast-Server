package lan.dk.podcastserver.scheduled

import com.github.davinkevin.podcastserver.scheduled.DatabaseScheduled
import com.github.davinkevin.podcastserver.service.DatabaseService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.scheduling.annotation.Scheduled

/**
 * Created by kevin on 29/03/2016 for Podcast Server
 */
@ExtendWith(MockitoExtension::class)
class DatabaseScheduledTest {

    @Mock lateinit var databaseService: DatabaseService
    @InjectMocks lateinit var databaseScheduled: DatabaseScheduled

    @Test
    fun should_launch_backup() {
        /* When */
        databaseScheduled.backup()

        /* Then */
        verify(databaseService).backupWithDefault()
    }

    @Test
    fun should_be_scheduled_for_4am_or_by_property() {
        /* Given */

        /* When */
        val cronValue = (DatabaseScheduled::class.java.getMethod("backup")
                .declaredAnnotations[0] as Scheduled).cron

        /* Then */
        assertThat(cronValue).contains("\${podcastserver.backup.cron:", "0 0 4 * * *", "}")
    }

}
