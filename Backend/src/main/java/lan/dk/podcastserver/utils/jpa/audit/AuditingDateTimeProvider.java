package lan.dk.podcastserver.utils.jpa.audit;

import org.springframework.data.auditing.DateTimeProvider;

import java.util.Calendar;
import java.util.GregorianCalendar;

import static java.time.ZonedDateTime.now;

/**
 * Created by kevin on 23/12/2015 for Podcast Server
 */
public class AuditingDateTimeProvider implements DateTimeProvider {

    @Override
    public Calendar getNow() {
        return GregorianCalendar.from(now());
    }
}
