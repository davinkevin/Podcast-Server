package lan.dk.podcastserver.service;

import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
        return Try.of(() -> p.getClass().getSimpleName())
            .filter(className -> className.contains("UNIXProcess"))
            .mapTry(className -> p.getClass().getDeclaredField("pid"))
            .andThenTry(c -> c.setAccessible(true))
            .mapTry(c -> c.getInt(p))
            .getOrElse(-1);
    }

    public Try<Integer> waitFor(Process process) {
        return Try.of(process::waitFor);
    }
}
