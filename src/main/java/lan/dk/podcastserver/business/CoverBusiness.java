package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.CoverRepository;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

/**
 * Created by kevin on 08/06/2014 for Podcast-Server
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class CoverBusiness {

    private static final String API_COVER = "/api/podcast/%s/cover.%s";

    final CoverRepository coverRepository;
    final PodcastServerParameters podcastServerParameters;
    final UrlService urlService;

    public Cover findOne(UUID id) {
        return coverRepository.findOne(id);
    }

    public String download(Podcast podcast) {

        if (podcast.getCover() == null || StringUtils.isEmpty(podcast.getCover().getUrl())) {
            return "";
        }

        if (podcast.getCover().getUrl().startsWith("/"))
            return podcast.getCover().getUrl();

        String coverUrl = podcast.getCover().getUrl();

        String fileName = podcastServerParameters.getCoverDefaultName() + "." + FilenameUtils.getExtension(coverUrl);

        Path fileLocation = podcastServerParameters.getRootfolder().resolve(podcast.getTitle()).resolve(fileName);

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
            return String.format(API_COVER, podcast.getId().toString(), FilenameUtils.getExtension(coverUrl));
        } catch (IOException e) {
            log.error("Error during downloading of the cover", e);
            return "";
        }
    }

    Boolean hasSameCoverURL(Podcast patchPodcast, Podcast podcastToUpdate) {
        return !Objects.isNull(patchPodcast.getCover()) && !Objects.isNull(podcastToUpdate.getCover()) &&
                patchPodcast.getCover().equals(podcastToUpdate.getCover());
    }

    Path getCoverPathOf(Podcast podcast) {
        String fileName = podcastServerParameters.getCoverDefaultName() + "." + FilenameUtils.getExtension(podcast.getCover().getUrl());
        return podcastServerParameters.getRootfolder().resolve(podcast.getTitle()).resolve(fileName);
    }

    public Cover save(Cover cover) {
        return coverRepository.save(cover);
    }
}
