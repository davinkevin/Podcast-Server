package lan.dk.podcastserver.service;

import com.google.common.collect.Lists;
import javaslang.control.Try;
import lan.dk.podcastserver.service.factory.ProcessBuilderFactory;
import lan.dk.podcastserver.utils.ThreadUtils.OutputLogger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Objects.nonNull;
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

    public void concatDemux(Path target, Path... files) {
        Path listOfFiles = null;
        try {
            Files.deleteIfExists(target);

            String filesStrings = Lists.newArrayList(files)
                    .stream()
                    .map(f -> f.getFileName().toString())
                    .map(p -> "file '" + p + "'")
                    .collect(joining(System.getProperty("line.separator")));

            listOfFiles = Files.createTempFile(target.getParent(), "ffmpeg-list", ".txt");
            Files.write(listOfFiles, filesStrings.getBytes());

            ProcessBuilder pb = processBuilderFactory.newProcessBuilder(ffmpeg,
                    /*"-v", "verbose",*/
                    "-f", "concat",
                    "-i", listOfFiles.toAbsolutePath().toString(),
                    "-vcodec", "copy",
                    "-acodec", "copy",
                    target.toAbsolutePath().toString()
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
        } finally {
            Path finalListOfFiles = listOfFiles;
            if (nonNull(listOfFiles)) Try.of(() -> Files.deleteIfExists(finalListOfFiles)); }
    }

}
