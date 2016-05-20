package lan.dk.podcastserver.utils.jackson;

import com.fasterxml.jackson.datatype.hibernate4.Hibernate4Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 15/07/15 for Podcast Server
 */
public class CustomObjectMapperTest {
    
    @Test
    public void should_configure_jackson_Object_mapper() {
        /* Given */
        CustomObjectMapperExtended mapper = new CustomObjectMapperExtended();

        /* When */
        Set<Object> registeredModuleTypes = mapper.getRegisteredModuleTypes();

        /* Then */
        assertThat(registeredModuleTypes)
                .hasSize(2)
                .contains(Hibernate4Module.class.getCanonicalName())
                .contains(JavaTimeModule.class.getCanonicalName());
    }

    static class CustomObjectMapperExtended extends CustomObjectMapper {

        public Set<Object> getRegisteredModuleTypes() {
            return _registeredModuleTypes;
        }
    }

}