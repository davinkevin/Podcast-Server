package lan.dk.podcastserver.config;

import org.junit.Before;
import org.junit.Test;
import org.springframework.core.convert.converter.Converter;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.nio.file.Path;
import java.nio.file.Paths;

import static lan.dk.podcastserver.service.MimeTypeService.TikaProbeContentType;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 13/08/15 for Podcast Server
 */
public class BeanConfigScanTest {

    private BeanConfigScan beanConfigScan;

    @Before
    public void beforeEach() {
        beanConfigScan = new BeanConfigScan();
    }

    @Test
    public void should_get_validator() {
        /* Given */
        /* When */
        LocalValidatorFactoryBean validator = beanConfigScan.validator();
        /* Then */
        assertThat(validator)
                .isNotNull()
                .isOfAnyClassIn(LocalValidatorFactoryBean.class);
    }

    @Test
    public void should_have_tika_probecontentType() throws NoSuchMethodException {
        /* Given */
        /* When */
        TikaProbeContentType tikaProbeContentType = beanConfigScan.tikaProbeContentType();

        /* Then */
        assertThat(tikaProbeContentType)
                .isNotNull()
                .isOfAnyClassIn(TikaProbeContentType.class);
    }

    @Test
    public void should_provide_a_converter_from_string_to_path() {
        /* Given */
        Converter<String, Path> c = beanConfigScan.pathConverter();
        String path = "/tmp";

        /* When */
        Path convertedPath = c.convert(path);

        /* Then */
        assertThat(convertedPath).isEqualTo(Paths.get(path));
    }
}