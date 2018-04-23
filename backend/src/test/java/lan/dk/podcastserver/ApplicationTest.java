package lan.dk.podcastserver;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lan.dk.podcastserver.service.JsonService;
import lombok.Getter;
import lombok.Setter;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

/**
 * Created by kevin on 15/06/2016 for Podcast Server
 */
@Ignore("Should have FFmpeg installed to be executed")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class, webEnvironment=DEFINED_PORT)
public class ApplicationTest {

    @Autowired JsonService jsonService;
    @Value("${server.port}") Integer port;

    @Test
    public void should_respond_true_to_health_check() throws IOException {
        /* Given */
        String health = "http://localhost:"+ port +"/system/health";
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
