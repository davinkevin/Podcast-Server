package lan.dk.podcastserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.Setter;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 15/06/2016 for Podcast Server
 */
@Ignore("Should have FFmpeg installed to be executed")
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebIntegrationTest("server.port:9000")
public class ApplicationTest {

    @Autowired JsonService jsonService;

    @Test
    public void should_respond_true_to_health_check() throws IOException {
        /* Given */
        String health = "http://localhost:9000/system/health";
        /* When */
        ApplicationHealthStatus status = jsonService.parseUrl(health).map(d -> d.read("$", ApplicationHealthStatus.class)).getOrElseThrow(RuntimeException::new);
        /* Then */
        assertThat(status).isNotNull();
        assertThat(status.getStatus()).isEqualTo("UP");
        assertThat(status.getDb().getStatus()).isEqualTo("UP");
        assertThat(status.getDiskSpace().getStatus()).isEqualTo("UP");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ApplicationHealthStatus extends HealthStatus {
        @Getter @Setter private DiskSpaceStatus diskSpace;
        @Getter @Setter private DbStatus db;

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class DiskSpaceStatus extends HealthStatus {}
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class DbStatus extends HealthStatus {}
    }

    public static class HealthStatus {
        @Getter @Setter private String status;
    }

}