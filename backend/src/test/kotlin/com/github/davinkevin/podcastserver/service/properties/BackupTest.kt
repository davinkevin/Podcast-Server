package com.github.davinkevin.podcastserver.service.properties

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Paths

/**
 * Created by kevin on 13/04/2016 for Podcast Server
 */
class BackupTest {

    @Test
    fun should_have_default_value() {
        /* Given */
        /* When */
        val backup = Backup()
        /* Then */

        assertThat(backup.location).isEqualTo(Paths.get("/tmp"))
        assertThat(backup.cron).isEqualTo("0 0 4 * * *")
        assertThat(backup.binary).isEqualTo(false)
    }

    @Test
    fun should_have_specified_values() {
        /* Given */
        /* When */
        val backup = Backup(
                binary = true,
                cron = "cron",
                location = Paths.get("/foo")
        )
        /* Then */
        assertThat(backup.binary).isEqualTo(true)
        assertThat(backup.cron).isEqualTo("cron")
        assertThat(backup.location).isEqualTo(Paths.get("/foo"))
    }

}
