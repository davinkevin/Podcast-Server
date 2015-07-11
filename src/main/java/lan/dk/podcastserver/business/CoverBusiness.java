package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.CoverRepository;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.utils.URLUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;

/**
 * Created by kevin on 08/06/2014.
 */
@Component
@Transactional
public class CoverBusiness {
    
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String QUOTE_CHARACTER = "'";
    public static final String QUOTE_HTML_REPLACEMENT = "%27";

    @Resource CoverRepository coverRepository;
    @Resource PodcastServerParameters podcastServerParameters;

    public Cover findOne(Integer integer) {
        return coverRepository.findOne(integer);
    }

    public Boolean exists(Integer integer) {
        return coverRepository.exists(integer);
    }

    public Cover save(Cover cover) { return coverRepository.save(cover); }

    public String download(Podcast podcast) {

        if (podcast.getCover() == null || StringUtils.isEmpty(podcast.getCover().getUrl())) {
            return "";
        }

        String coverUrl = podcast.getCover().getUrl();
        String fileName = podcastServerParameters.coverDefaultName() + "." + FilenameUtils.getExtension(coverUrl);

        Path fileLocation = podcastServerParameters.rootFolder().resolve(podcast.getTitle()).resolve(fileName);

        // Download file to correct location :
        try {

            if (!Files.exists(fileLocation.getParent())) {
                Files.createDirectories(fileLocation.getParent());
            }

            URLConnection urlConnection = URLUtils.getConnectionWithTimeOut(coverUrl, 5000);

            Files.copy(
                    urlConnection.getInputStream(),
                    fileLocation,
                    StandardCopyOption.REPLACE_EXISTING
            );
            return UriComponentsBuilder.fromUri(podcastServerParameters.fileContainer())
                    .pathSegment(podcast.getTitle())
                    .pathSegment(fileName)
                    .build()
                    .toUriString()
                    .replace(QUOTE_CHARACTER, QUOTE_HTML_REPLACEMENT);
        } catch (URISyntaxException | IOException e) {
            logger.error("Error during downloading of the cover", e);
            return "";
        }
    }

    public Boolean hasSameCoverURL(Podcast patchPodcast, Podcast podcastToUpdate) {
        return !Objects.isNull(patchPodcast.getCover()) && !Objects.isNull(podcastToUpdate.getCover()) && StringUtils.equalsIgnoreCase(patchPodcast.getCover().getUrl(), podcastToUpdate.getCover().getUrl());
    }
}
