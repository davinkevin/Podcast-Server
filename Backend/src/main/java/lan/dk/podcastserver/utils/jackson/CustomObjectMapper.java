package lan.dk.podcastserver.utils.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.stereotype.Service;

@Service
public class CustomObjectMapper extends ObjectMapper {

    public CustomObjectMapper() {
        registerModule(hibernate4Module());
        registerModule(new JavaTimeModule());

        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    }

    private Hibernate4Module hibernate4Module() {
        return new Hibernate4Module()
                .configure(Hibernate4Module.Feature.USE_TRANSIENT_ANNOTATION, false)
                .configure(Hibernate4Module.Feature.FORCE_LAZY_LOADING, true);
    }

}
