package lan.dk.podcastserver.utils.custom.ffmpeg;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 24/07/2016.
 */
@Slf4j
@Setter @Getter
@Accessors(fluent = true, chain = true)
@RequiredArgsConstructor
public class ProcessListener {

    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    public static final ProcessListener DEFAULT_PROCESS_LISTENER = new ProcessListener(StringUtils.EMPTY);

    private final String url;
    private Process process;

    public Future<Process> getProcess() {
        return pool.submit(() -> {
            while (true) {
                if(nonNull(process)) return process;
                TimeUnit.MILLISECONDS.sleep(100);
            }
        });
    }
}
