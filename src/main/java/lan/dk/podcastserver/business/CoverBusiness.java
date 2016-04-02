package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.CoverRepository;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.UrlService;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by kevin on 08/06/2014 for Podcast-Server
 */
@Component
@Transactional
public class CoverBusiness {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    final CoverRepository coverRepository;
    final PodcastServerParameters podcastServerParameters;
    final UrlService urlService;

    @Autowired
    public CoverBusiness(CoverRepository coverRepository, PodcastServerParameters podcastServerParameters, UrlService urlService) {
        this.coverRepository = coverRepository;
        this.podcastServerParameters = podcastServerParameters;
        this.urlService = urlService;
    }

    public Cover findOne(UUID id) {
        return coverRepository.findOne(id);
    }

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

            URLConnection urlConnection = urlService.getConnectionWithTimeOut(coverUrl, 5000);

            Files.copy(
                    urlConnection.getInputStream(),
                    fileLocation,
                    StandardCopyOption.REPLACE_EXISTING
            );
            return UriComponentsBuilder.fromUri(podcastServerParameters.serverUrl())
                    .pathSegment("api", "podcast", podcast.getId().toString(), "cover." + FilenameUtils.getExtension(coverUrl))
                    .build()
                    .toUriString();
        } catch (URISyntaxException | IOException e) {
            logger.error("Error during downloading of the cover", e);
            return "";
        }
    }

    public Boolean hasSameCoverURL(Podcast patchPodcast, Podcast podcastToUpdate) {
        return !Objects.isNull(patchPodcast.getCover()) && !Objects.isNull(podcastToUpdate.getCover()) &&
                patchPodcast.getCover().equals(podcastToUpdate.getCover());
    }

    public Path getCoverPathOf(Podcast podcast) {
        String fileName = podcastServerParameters.coverDefaultName() + "." + FilenameUtils.getExtension(podcast.getCover().getUrl());
        return podcastServerParameters.rootFolder().resolve(podcast.getTitle()).resolve(fileName);
    }

    public Cover save(Cover cover) {
        return coverRepository.save(cover);
    }
}
