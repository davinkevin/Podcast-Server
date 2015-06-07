package lan.dk.podcastserver.Utils;

import lan.dk.podcastserver.context.UtilsConfig;
import lan.dk.podcastserver.service.FfmpegService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.annotation.Resource;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 19/07/2014.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {/*PropertyConfig.class,*/ UtilsConfig.class}, loader=AnnotationConfigContextLoader.class)
public class FfmpegUtilsTest {

    @Resource
    FfmpegService ffmpegService;

    @Test
    public void testFfmpegConcatDemux() throws Exception {
        String location = "/Users/kevin/Tomcat/Tomcat 7/webapp/podcast/Devoxx France 2014/";

        List<File> fileList = new ArrayList<>();
        fileList.add(new File(location.concat("1.mp4")));
        fileList.add(new File(location.concat("2.mp4")));
        fileList.add(new File(location.concat("3.mp4")));

        ffmpegService.concatDemux(new File(location + "concat.mp4"), fileList.toArray(new File[fileList.size()]));
    }
}
