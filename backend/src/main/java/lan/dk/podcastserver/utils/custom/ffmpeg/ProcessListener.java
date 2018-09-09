package lan.dk.podcastserver.utils.custom.ffmpeg;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ProcessListener {

    private final ExecutorService pool = Executors.newFixedThreadPool(10);

    public static final ProcessListener DEFAULT_PROCESS_LISTENER = new ProcessListener(StringUtils.EMPTY);

    public final String url;
    private Process process;

    public Future<Process> findProcess() {
        return pool.submit(() -> {
            while (true) {
                if(nonNull(process)) return process;
                TimeUnit.MILLISECONDS.sleep(100);
            }
        });
    }

    public void setProcess(Process process) {
        this.process = process;
    }

    public String getUrl() {
        return this.url;
    }
    public Process getProcess() { return process; }
}
