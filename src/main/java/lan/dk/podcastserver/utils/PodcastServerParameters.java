package lan.dk.podcastserver.utils;

import org.springframework.core.env.Environment;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by kevin on 03/02/15.
 */
/*@Service*/
public class PodcastServerParameters {

    String rootfolder, serveurURL, fileContainer, coverDefaultName, downloadExtention;
    Integer maxUpdateParallels, concurrentDownload, numberOfTry;
    Long numberOfDayToDownload;

    public PodcastServerParameters(String rootfolder, String serveurURL, String fileContainer, String coverDefaultName, String downloadExtention, Long numberOfDayToDownload, Integer maxUpdateParallels, Integer concurrentDownload, Integer numberOfTry) {
        this.rootfolder = rootfolder;
        this.serveurURL = serveurURL;
        this.fileContainer = fileContainer;
        this.downloadExtention = downloadExtention;
        this.coverDefaultName = coverDefaultName;
        this.numberOfDayToDownload = numberOfDayToDownload;
        this.maxUpdateParallels = maxUpdateParallels;
        this.concurrentDownload = concurrentDownload;
        this.numberOfTry = numberOfTry;
    }

    //** GETTER OF THE PARAMETERS **//
    public String getRootfolder() {
        return rootfolder;
    }
    public String getServeurURL() {
        return serveurURL;
    }
    public String getFileContainer() {
        return fileContainer;
    }
    public String getCoverDefaultName() { return coverDefaultName; }
    public String getDownloadExtention() {
        return downloadExtention;
    }
    public Long getNumberOfDayToDownload() { return numberOfDayToDownload; }
    public Integer getMaxUpdateParallels() { return maxUpdateParallels; }

    public Path rootFolder() { return Paths.get(rootfolder); }
    public URI serveurURL() throws URISyntaxException { return new URI(serveurURL); }
    public URI fileContainer() throws URISyntaxException { return new URI(fileContainer); }
    public Long numberOfDayToDownload() { return numberOfDayToDownload; }
    public Integer maxUpdateParallels() { return maxUpdateParallels; }
    public Integer concurrentDownload() { return concurrentDownload; }
    public Integer numberOfTry() {return numberOfTry;}
    public String coverDefaultName() { return coverDefaultName;}

    public static PodcastServerParametersBuilder builder(Environment env) {
        return new PodcastServerParametersBuilder(env);
    }
    
    public static class PodcastServerParametersBuilder {
        String rootfolder, serveurURL, fileContainer, coverDefaultName, downloadExtention;
        Integer maxUpdateParallels, concurrentDownload, numberOfTry;
        Long numberOfDayToDownload;

        ExpressionParser parser = new SpelExpressionParser();
        EvaluationContext context = new StandardEvaluationContext();


        public PodcastServerParametersBuilder(Environment environment) {
            context.setVariable("environment", environment);
        }

        public PodcastServerParametersBuilder rootfolder(String rootfolder) {
            this.rootfolder = parser.parseExpression(rootfolder).getValue(context, String.class);
            return this;
        }

        public PodcastServerParametersBuilder serveurURL(String serveurURL) {
            this.serveurURL = serveurURL;
            return this;
        }

        public PodcastServerParametersBuilder fileContainer(String fileContainer) {
            this.fileContainer = fileContainer;
            return this;
        }

        public PodcastServerParametersBuilder coverDefaultName(String coverDefaultName) {
            this.coverDefaultName = coverDefaultName;
            return this;
        }

        public PodcastServerParametersBuilder numberOfDayToDownload(Long numberOfDayToDownload) {
            this.numberOfDayToDownload = numberOfDayToDownload;
            return this;
        }

        public PodcastServerParametersBuilder maxUpdateParallels(Integer maxUpdateParallels) {
            this.maxUpdateParallels = maxUpdateParallels;
            return this;
        }

        public PodcastServerParametersBuilder concurrentDownload(Integer concurrentDownload) {
            this.concurrentDownload = concurrentDownload;
            return this;
        }

        public PodcastServerParametersBuilder numberOfTry(Integer numberOfTry) {
            this.numberOfTry = numberOfTry;
            return this;
        }

        public PodcastServerParametersBuilder downloadExtention(String downloadExtention) {
            this.downloadExtention = downloadExtention;
            return this;
        }

        public PodcastServerParameters build() {
            return new PodcastServerParameters(rootfolder, serveurURL, fileContainer, coverDefaultName, downloadExtention, numberOfDayToDownload, maxUpdateParallels, concurrentDownload, numberOfTry);
        }
        
    }
}
