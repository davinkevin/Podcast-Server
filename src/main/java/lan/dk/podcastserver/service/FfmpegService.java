package lan.dk.podcastserver.service;

import com.google.common.collect.Lists;
import lan.dk.podcastserver.service.factory.ProcessBuilderFactory;
import lan.dk.podcastserver.utils.ThreadUtils.OutputLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;

import static java.util.stream.Collectors.joining;

/**
 * Created by kevin on 19/07/2014 for Podcast Server
 */
@Component("FfmpegService")
public class FfmpegService {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${podcastserver.externaltools.ffmpeg:/usr/bin/ffmpeg}")
    public String ffmpeg;

    @Autowired ProcessBuilderFactory processBuilderFactory;

    public void concatDemux (File target, File... files) {
        if (target.exists() && target.isFile())
            target.delete();

        try {
            String filesStrings = Lists.newArrayList(files)
                    .stream()
                    .map(File::getAbsolutePath)
                    .collect(joining("|"));

            ProcessBuilder pb = processBuilderFactory.newProcessBuilder(ffmpeg,
                    /*"-v", "verbose",*/
                    "-i", "concat:" + filesStrings,
                    "-c", "copy", target.getAbsolutePath()
            )
                    .directory(new File("/tmp"))
                    /*.inheritIO()*/;

            logger.debug(String.valueOf(pb.command()));
            Process p = pb.start();

            Thread output = new Thread(new OutputLogger(p.getInputStream()));
            Thread error = new Thread(new OutputLogger(p.getErrorStream()));

            output.start();
            error.start();
            p.waitFor();

            output.interrupt();
            error.interrupt();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

}
