package lan.dk.podcastserver.utils;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;

public class DateUtils {

    public static final String UPLOAD_PATTERN = "yyyy-MM-dd";
    public static final String CANALPLUS_PATTERN = "dd/MM/yyyy-HH:mm:ss";
    public static final String BEINSPORT_PATTERN = "MMM dd yyyy, HH:mm";
    public static final String JEUXVIDEOFR_PATTERN = "dd/MM/yyyy";
    public static final String PARLEYS_PATTERN = "EEE MMM dd HH:mm:ss z yyyy";

    public static ZonedDateTime fromRFC822(String pubDate) {
        return ZonedDateTime.parse(pubDate, DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public static ZonedDateTime fromCanalPlus(String date, String heure) {
        LocalDateTime localDateTime = LocalDateTime.parse(date.concat("-").concat(heure), DateTimeFormatter.ofPattern(CANALPLUS_PATTERN));
        return ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Paris"));
    }

    public static ZonedDateTime fromYoutube(String pubDate) {
        return ZonedDateTime.parse(pubDate, DateTimeFormatter.ISO_DATE_TIME); //2013-12-20T22:30:01.000Z
    }

    public static ZonedDateTime fromFolder(String pubDate) {
        return ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(pubDate, DateTimeFormatter.ofPattern(UPLOAD_PATTERN)), LocalTime.of(0, 0)), ZoneId.systemDefault());
    }

    public static String toRFC2822(ZonedDateTime zonedDateTime) {
        return zonedDateTime.format(DateTimeFormatter.RFC_1123_DATE_TIME);
    }

    public static String TimeStampToRFC2822 (Timestamp timestamp) {
        return (timestamp != null)
                ? new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(timestamp)
                : new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH).format(new Timestamp(new Date().getTime()));
    }

    public static ZonedDateTime fromBeInSport(String beInSportDate) {
        LocalDateTime localDateTime = LocalDateTime.parse(beInSportDate, DateTimeFormatter.ofPattern(BEINSPORT_PATTERN, Locale.ENGLISH)); // Format : Feb 17 2014, 10:26
        return ZonedDateTime.of(localDateTime, ZoneId.of("Europe/Paris"));
    }

    public static ZonedDateTime fromJeuxVideoFr(String pubDate) {
        return ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(pubDate, DateTimeFormatter.ofPattern(JEUXVIDEOFR_PATTERN)), LocalTime.of(0, 0)), ZoneId.of("Europe/Paris"));
    }

    public static ZonedDateTime fromParleys(String pubDate) {
        return ZonedDateTime.parse(pubDate, DateTimeFormatter.ofPattern(PARLEYS_PATTERN, Locale.ENGLISH)); // Format : Thu Jun 26 06:34:41 UTC 2014
    }

    public static ZonedDateTime fromPluzz(Long dateInSecondsSinceEpoch){
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(dateInSecondsSinceEpoch), ZoneId.of("Europe/Paris"));
    }
}
