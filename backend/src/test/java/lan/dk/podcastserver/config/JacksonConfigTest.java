package lan.dk.podcastserver.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 15/06/2016 for Podcast Server
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(classes = {JacksonConfig.class})
public class JacksonConfigTest {

    @Autowired ObjectMapper mapper;

    @Test
    public void should_serialize_date_as_nanosec() throws JsonProcessingException {
        /* Given */
        ZonedDateTime date = ZonedDateTime.of(2016, 6, 15, 2, 43, 15, 826, ZoneId.of("Europe/Paris"));
        /* When */
        String itemAsJson = mapper.writeValueAsString(date);
        /* Then */
        assertThat(itemAsJson).isEqualTo("\"2016-06-15T02:43:15.000000826+02:00\"");
    }
}
