package lan.dk.podcastserver.service.factory;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Created by kevin on 25/01/2016 for Podcast Server
 */
@Service
public class ProcessBuilderFactory {

    public ProcessBuilder newProcessBuilder(String... command) { return new ProcessBuilder(command); }
    public ProcessBuilder newProcessBuilder(List<String> command) {
        return new ProcessBuilder(command);
    }

}
