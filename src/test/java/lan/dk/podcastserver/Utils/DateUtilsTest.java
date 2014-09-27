package lan.dk.podcastserver.Utils;

import lan.dk.podcastserver.utils.DateUtils;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * Created by kevin on 27/09/2014.
 */
public class DateUtilsTest {

    private final Logger logger = LoggerFactory.getLogger(DateUtilsTest.class);

    @Test
    public void should_get_the_right_localdatetime_from_upload_format () {
        ZonedDateTime zonedDateTime = DateUtils.fromFolder("2014-12-21");

        assertThat(zonedDateTime.getYear()).isEqualTo(2014);
        assertThat(zonedDateTime.getMonthValue()).isEqualTo(12);
        assertThat(zonedDateTime.getDayOfMonth()).isEqualTo(21);
        assertThat(zonedDateTime.getHour()).isEqualTo(0);
        assertThat(zonedDateTime.getMinute()).isEqualTo(0);
        assertThat(zonedDateTime.getSecond()).isEqualTo(0);
    }
    
    @Test
    public void should_parse_a_RFC822_date () {
        ZonedDateTime zonedDateTime = DateUtils.fromRFC822("Sun, 21 Dec 2014 11:05:30 GMT");

        assertThat(zonedDateTime.getYear()).isEqualTo(2014);
        assertThat(zonedDateTime.getMonthValue()).isEqualTo(12);
        assertThat(zonedDateTime.getDayOfMonth()).isEqualTo(21);
        assertThat(zonedDateTime.getHour()).isEqualTo(11);
        assertThat(zonedDateTime.getMinute()).isEqualTo(5);
        assertThat(zonedDateTime.getSecond()).isEqualTo(30);

    }

    @Test
    public void should_parse_a_canalplus_date () {
        ZonedDateTime zonedDateTime = DateUtils.fromCanalPlus("21/12/2014", "11:05:30");

        assertThat(zonedDateTime.getYear()).isEqualTo(2014);
        assertThat(zonedDateTime.getMonthValue()).isEqualTo(12);
        assertThat(zonedDateTime.getDayOfMonth()).isEqualTo(21);
        assertThat(zonedDateTime.getHour()).isEqualTo(11);
        assertThat(zonedDateTime.getMinute()).isEqualTo(5);
        assertThat(zonedDateTime.getSecond()).isEqualTo(30);
    }

    @Test
    public void should_parse_youtube_date () {
        ZonedDateTime zonedDateTime = DateUtils.fromYoutube("2014-12-21T11:05:30.000Z");

        assertThat(zonedDateTime.getYear()).isEqualTo(2014);
        assertThat(zonedDateTime.getMonthValue()).isEqualTo(12);
        assertThat(zonedDateTime.getDayOfMonth()).isEqualTo(21);
        assertThat(zonedDateTime.getHour()).isEqualTo(11);
        assertThat(zonedDateTime.getMinute()).isEqualTo(5);
        assertThat(zonedDateTime.getSecond()).isEqualTo(30);
    }

    @Test
    public void should_output_RFC2822_date () {
        String date = "Sun, 21 Dec 2014 11:05:30 GMT";
        ZonedDateTime zonedDateTime = DateUtils.fromRFC822(date);
        assertThat(date).isEqualTo(DateUtils.toRFC2822(zonedDateTime));
    }

    @Test
    public void should_parse_beinsport_date () {
        ZonedDateTime zonedDateTime = DateUtils.fromBeInSport("Dec 21 2014, 11:05");

        assertThat(zonedDateTime.getYear()).isEqualTo(2014);
        assertThat(zonedDateTime.getMonthValue()).isEqualTo(12);
        assertThat(zonedDateTime.getDayOfMonth()).isEqualTo(21);
        assertThat(zonedDateTime.getHour()).isEqualTo(11);
        assertThat(zonedDateTime.getMinute()).isEqualTo(5);
    }

    @Test
    public void should_parse_jeuxvideofr_date () {
        ZonedDateTime zonedDateTime = DateUtils.fromJeuxVideoFr("21/12/2014");

        assertThat(zonedDateTime.getYear()).isEqualTo(2014);
        assertThat(zonedDateTime.getMonthValue()).isEqualTo(12);
        assertThat(zonedDateTime.getDayOfMonth()).isEqualTo(21);
    }

    @Test
    public void should_parse_parleys_date () {
        ZonedDateTime zonedDateTime = DateUtils.fromParleys("Sun Dec 21 11:05:30 UTC 2014");

        assertThat(zonedDateTime.getYear()).isEqualTo(2014);
        assertThat(zonedDateTime.getMonthValue()).isEqualTo(12);
        assertThat(zonedDateTime.getDayOfMonth()).isEqualTo(21);
        assertThat(zonedDateTime.getHour()).isEqualTo(11);
        assertThat(zonedDateTime.getMinute()).isEqualTo(5);
        assertThat(zonedDateTime.getSecond()).isEqualTo(30);
    }

    @Test
    public void should_parse_pluzz_date () {
        ZonedDateTime zonedDateTime = DateUtils.fromPluzz(1419156330L);

        assertThat(zonedDateTime.getYear()).isEqualTo(2014);
        assertThat(zonedDateTime.getMonthValue()).isEqualTo(12);
        assertThat(zonedDateTime.getDayOfMonth()).isEqualTo(21);
        assertThat(zonedDateTime.getHour()).isEqualTo(11);
        assertThat(zonedDateTime.getMinute()).isEqualTo(5);
        assertThat(zonedDateTime.getSecond()).isEqualTo(30);
    }
}
