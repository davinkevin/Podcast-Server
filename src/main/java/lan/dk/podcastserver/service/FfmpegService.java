package lan.dk.podcastserver.service;

import com.google.common.collect.Lists;
import lan.dk.podcastserver.service.factory.ProcessBuilderFactory;
import lan.dk.podcastserver.utils.ThreadUtils.OutputLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.stream.Collectors.joining;

/**
 * Created by kevin on 19/07/2014 for Podcast Server
 */
@Slf4j
@Component("FfmpegService")
public class FfmpegService {

    private final Path workingDirectory = Paths.get("/tmp");

    @Value("${podcastserver.externaltools.ffmpeg:/usr/bin/ffmpeg}")
    public String ffmpeg;

    @Autowired ProcessBuilderFactory processBuilderFactory;

    public void concatDemux (File target, File... files) {
        try {
            Files.deleteIfExists(target.toPath());

            String filesStrings = Lists.newArrayList(files)
                    .stream()
                    .map(File::getAbsolutePath)
                    .collect(joining("|"));

            ProcessBuilder pb = processBuilderFactory.newProcessBuilder(ffmpeg,
                    /*"-v", "verbose",*/
                    "-i", "concat:" + filesStrings,
                    "-c", "copy", target.getAbsolutePath()
            )
                    .directory(workingDirectory.toFile())
                    .redirectErrorStream(true)
                    /*.inheritIO()*/;

            log.debug(String.valueOf(pb.command()));
            Process p = pb.start();

            Thread output = new Thread(new OutputLogger(p.getInputStream()));

            output.start();
            p.waitFor();

            output.interrupt();
        } catch (IOException | InterruptedException e) {
            log.error("Error during Ffmpeg conversion", e);
        }
    }

}
