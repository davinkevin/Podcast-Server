package lan.dk.podcastserver.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

@Controller
@RequestMapping("/download")
public class downloadController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${rootfolder}")
    protected String rootFolder;

    @Value("${serverURL}")
    protected String serverURL;

    @RequestMapping(value = "/{podcastname}/{filename:.*}", method = RequestMethod.GET)
    public void downloadFile(@PathVariable String podcastname, @PathVariable String filename, HttpServletResponse response) {

        logger.info(podcastname);
        logger.info(filename);

        String fileLocation = rootFolder + File.separator + podcastname.concat(File.separator + filename).replaceAll("%20", " ");
        logger.debug(fileLocation);
        File episodeToDownload = new File(fileLocation);

        if (episodeToDownload.exists()) {
            String destination = serverURL + "podcast" + "/" + podcastname + "/" + filename;
            try {
                response.sendRedirect(destination.replaceAll(" ", "%20"));
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        } else {
            logger.info("L'épisode n'existe pas");
            try {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
    }

}
