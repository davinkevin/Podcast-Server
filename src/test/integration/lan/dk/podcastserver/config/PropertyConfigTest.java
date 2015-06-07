package lan.dk.podcastserver.config;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {/*PropertyConfig.class*/}, loader=AnnotationConfigContextLoader.class)
public class PropertyConfigTest {
    
    @Value("#{systemEnvironment['HOME']}")
    String home;

    @Value("#{systemEnvironment['PATH']}")
    String path;
    
    @Test
    public void should_show_my_home () {
        assertThat(home).isEqualTo("/Users/kevin");
    }

    @Test
    public void should_check_my_path () {
        assertThat(path).isEqualTo("/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/X11/bin:/usr/local/git/bin");
    }


}
