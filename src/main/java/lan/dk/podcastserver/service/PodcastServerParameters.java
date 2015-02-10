package lan.dk.podcastserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by kevin on 03/02/15.
 */
@Service
public class PodcastServerParameters {

    @Value("${rootfolder:${catalina.home}/webapps/podcast/}") String rootfolder;
    @Value("${serverURL:http://localhost:8080}") String serveurURL;
    @Value("${fileContainer:http://localhost:8080/podcast}") String fileContainer;
    @Value("${cover.defaultname:cover}") String coverDefaultName;
    @Value("${download.extention:.psdownload}") String downloadExtention;
    
    @Value("${maxUpdateParallels:3}") Integer maxUpdateParallels;
    @Value("${concurrentDownload:3}") Integer concurrentDownload;
    @Value("${numberOfTry:10}") Integer numberOfTry;

    @Value("${numberofdaytodownload:30}") Long numberOfDayToDownload;

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
    public String rootFolderWithProtocol() { return "file://".concat(rootfolder); }
    public URI serveurURL() throws URISyntaxException { return new URI(serveurURL); }
    public URI fileContainer() throws URISyntaxException { return new URI(fileContainer); }
    public Long numberOfDayToDownload() { return numberOfDayToDownload; }
    public Integer maxUpdateParallels() { return maxUpdateParallels; }
    public Integer concurrentDownload() { return concurrentDownload; }
    public Integer numberOfTry() {return numberOfTry;}
    public String coverDefaultName() { return coverDefaultName;}
}
