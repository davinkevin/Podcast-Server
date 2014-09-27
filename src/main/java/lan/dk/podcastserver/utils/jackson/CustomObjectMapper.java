package lan.dk.podcastserver.utils.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.fasterxml.jackson.datatype.jsr310.JSR310Module;

public class CustomObjectMapper extends ObjectMapper {

    public CustomObjectMapper() {
        registerModule(hibernate4Module());
        registerModule(new JSR310Module());

        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, true);
        configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    }

    private Hibernate4Module hibernate4Module() {
        return new Hibernate4Module()
                .configure(Hibernate4Module.Feature.USE_TRANSIENT_ANNOTATION, false);
    }

}
