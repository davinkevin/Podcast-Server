package lan.dk.podcastserver.service;

import javaslang.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;

/**
 * Created by kevin on 25/01/2016 for Podcast Server
 */
@Slf4j
@Service
public class ProcessService {

    public ProcessBuilder newProcessBuilder(String... command) { return new ProcessBuilder(command); }

    public Try<Process> start(ProcessBuilder processBuilder) {
        return Try.of(processBuilder::start);
    }

    public int pidOf(Process p) {
        try {
            if (p.getClass().getSimpleName().contains("UNIXProcess")) {
                Field pidField = p.getClass().getDeclaredField("pid");
                pidField.setAccessible(true);
                return pidField.getInt(p);
            }
        } catch (IllegalAccessException | NoSuchFieldException ignored) {}
        return -1;
    }

    public Try<Integer> waitFor(Process process) {
        return Try.of(process::waitFor);
    }
}
