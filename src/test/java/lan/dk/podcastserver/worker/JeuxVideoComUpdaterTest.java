package lan.dk.podcastserver.worker;

import lan.dk.podcastserver.context.ValidatorConfig;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.manager.worker.updater.JeuxVideoComUpdater;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Resource;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {/*PropertyConfig.class,*/ ValidatorConfig.class}, loader=AnnotationConfigContextLoader.class)
public class JeuxVideoComUpdaterTest {

    @Resource
    JeuxVideoComUpdater jeuxVideoComUpdater;

    Podcast SPEED_GAME;

    @Before
    public void beforeEach() {
        SPEED_GAME = new Podcast();
        SPEED_GAME.setTitle("Speed Game");
        SPEED_GAME.setType("JeuxVideoCom");
        SPEED_GAME.setUrl("http://www.jeuxvideo.com/chroniques-video-speed-game.htm");
    }

    @Test
    public void should_find_the_same_signature () {
        String signature = jeuxVideoComUpdater.generateSignature(SPEED_GAME);
        String signature2 = jeuxVideoComUpdater.generateSignature(SPEED_GAME);

        assertThat(signature).isEqualTo(signature2);
    }

    @Test
    public void should_find_all_episodes () {
        jeuxVideoComUpdater.getItems(SPEED_GAME);

    }



}
