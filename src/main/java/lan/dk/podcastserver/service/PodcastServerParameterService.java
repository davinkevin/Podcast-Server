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
public class PodcastServerParameterService {

    @Value("${rootfolder:${catalina.home}/webapp/podcast/}") String rootfolder;
    @Value("${serverURL:http://localhost:8080}") String serveurURL;
    @Value("${fileContainer:http://localhost:8080/podcast}") String fileContainer;

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

    public Path rootFolder() { return Paths.get(rootfolder); }
    public URI serveurURL() throws URISyntaxException { return new URI(serveurURL); }
    public URI fileContainer() throws URISyntaxException { return new URI(fileContainer); }
}
