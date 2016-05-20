package lan.dk.podcastserver.service.factory;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.List;

/**
 * Created by kevin on 25/01/2016 for Podcast Server
 */
@Slf4j
@Service
public class ProcessBuilderFactory {

    public ProcessBuilder newProcessBuilder(String... command) { return new ProcessBuilder(command); }
    public ProcessBuilder newProcessBuilder(List<String> command) {
        return new ProcessBuilder(command);
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
}
