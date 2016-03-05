package lan.dk.podcastserver.service;

import lan.dk.podcastserver.service.factory.ProcessBuilderFactory;
import lan.dk.podcastserver.utils.ThreadUtils.OutputLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;

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
        File listOfFileToConcat = null;
        if (target.exists() && target.isFile())
            target.delete();

        try {

            listOfFileToConcat = getListingFile(target, files);

            ProcessBuilder pb = processBuilderFactory.newProcessBuilder(ffmpeg,
                    "-f",
                    "concat",
                    "-i",
                    "\"" + listOfFileToConcat.getAbsolutePath().replace("\'", "\\\'") + "\"",
                    "-c",
                    "copy",
                    target.getAbsolutePath());

            pb.directory(new File("/tmp"));
            logger.debug(String.valueOf(pb.command()));
            Process p = pb.start();

            Thread output = new Thread(new OutputLogger(p.getInputStream()));
            Thread error = new Thread(new OutputLogger(p.getErrorStream()));

            output.start();
            error.start();

            p.waitFor();

            output.interrupt();
            error.interrupt();

            if (p.getClass().getSimpleName().contains("UNIXProcess")) {
                Field pidField = p.getClass().getDeclaredField("pid");
                pidField.setAccessible(true);
                int pid = pidField.getInt(p);
                logger.debug("PID du process : " + pid);
            }


        } catch (IOException | IllegalAccessException | NoSuchFieldException | InterruptedException e) {
            e.printStackTrace();
        } finally {
            if (listOfFileToConcat != null)
                listOfFileToConcat.delete();
        }


    }

    private File getListingFile(File target, File... files) throws IOException {
        File listOfFileToConcat = File.createTempFile("Ffmpeg-listing", ".txt", target.getParentFile());

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(listOfFileToConcat)));
        for (File file: files) {
            writer.write("file '" + file.getAbsolutePath().replace("\'", "\\\'") + "'");
            writer.newLine();
        }
        writer.close();

        return listOfFileToConcat;
    }

}
