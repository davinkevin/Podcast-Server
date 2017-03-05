package lan.dk.podcastserver.business;

import com.mashape.unirest.http.HttpResponse;
import javaslang.collection.List;
import javaslang.control.Try;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.CoverRepository;
import lan.dk.podcastserver.service.UrlService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import static java.util.Objects.isNull;

/**
 * Created by kevin on 08/06/2014 for Podcast-Server
 */
@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class CoverBusiness {

    private final CoverRepository coverRepository;
    private final PodcastServerParameters podcastServerParameters;
    private final UrlService urlService;

    public Cover findOne(UUID id) {
        return coverRepository.findOne(id);
    }

    public String download(Podcast podcast) {

        if (podcast.getCover() == null || StringUtils.isEmpty(podcast.getCover().getUrl())) {
            return StringUtils.EMPTY;
        }

        if (podcast.getCover().getUrl().startsWith("/"))
            return podcast.getCover().getUrl();

        String coverUrl = podcast.getCover().getUrl();
        String fileName = podcastServerParameters.getCoverDefaultName() + "." + FilenameUtils.getExtension(coverUrl);
        Path fileLocation = podcastServerParameters.getRootfolder().resolve(podcast.getTitle()).resolve(fileName);

        // Download file to correct location :
        return Try.run(() -> createParentDirectory(fileLocation))
            .mapTry(v -> imageRequest(coverUrl))
            .andThenTry(saveToDisk(fileLocation))
            .map(v -> String.format(Podcast.COVER_PROXY_URL, podcast.getId().toString(), FilenameUtils.getExtension(coverUrl)))
            .onFailure(e -> log.error("Error during downloading of the cover", e))
            .getOrElse("");
    }

    public Boolean download(Item item) {
        if (isNull(item.getPodcast()) || isNull(item.getId())){
            log.error("Podcast or ID of item should not be null for element with title {}", item.getTitle());
            return false;
        }

        if (isNull(item.getCover()) || Cover.DEFAULT_COVER.equals(item.getCover()) || StringUtils.isEmpty(item.getCover().getUrl())) {
            log.error("Cover null or empty for item of id {}", item.getId());
            return false;
        }

        String coverUrl = item.getCover().getUrl();
        String fileName = item.getId() + "." + FilenameUtils.getExtension(coverUrl);
        Path fileLocation = podcastServerParameters.getRootfolder().resolve(item.getPodcast().getTitle()).resolve(fileName);

        return Try.run(() -> createParentDirectory(fileLocation))
                .mapTry(v -> imageRequest(coverUrl))
                .andThenTry(saveToDisk(fileLocation))
                .map(v -> Boolean.TRUE)
                .onFailure(e -> log.error("Error during downloading of the cover", e))
                .getOrElse(Boolean.FALSE);
    }

    Boolean hasSameCoverURL(Podcast patchPodcast, Podcast podcastToUpdate) {
        return !isNull(patchPodcast.getCover()) && !isNull(podcastToUpdate.getCover()) &&
                patchPodcast.getCover().equals(podcastToUpdate.getCover());
    }

    Path getCoverPathOf(Podcast podcast) {
        String fileName = podcastServerParameters.getCoverDefaultName() + "." + FilenameUtils.getExtension(podcast.getCover().getUrl());
        return podcastServerParameters.getRootfolder().resolve(podcast.getTitle()).resolve(fileName);
    }

    public Path getCoverPathOf(Item i) {
        String fileName = i.getId() + "." + FilenameUtils.getExtension(i.getCover().getUrl());
        return podcastServerParameters.getRootfolder().resolve(i.getPodcast().getTitle()).resolve(fileName);
    }

    public Cover save(Cover cover) {
        return coverRepository.save(cover);
    }

    private Try.CheckedConsumer<HttpResponse<InputStream>> saveToDisk(Path fileLocation) {
        return r -> Files.copy(
                r.getBody(),
                fileLocation,
                StandardCopyOption.REPLACE_EXISTING );
    }

    private HttpResponse<InputStream> imageRequest(String coverUrl) throws IOException {
        return Try.of(() -> urlService.get(coverUrl).asBinary())
                .filter(this::isImage)
                .getOrElseThrow(() -> new IOException("Not an image in content type"));
    }

    private Boolean isImage(HttpResponse<InputStream> request) {
        return List.ofAll(request.getHeaders().get("Content-Type"))
                .find(h -> h.contains("image"))
                .isDefined();
    }

    private void createParentDirectory(Path fileLocation) throws IOException {
        if (!Files.exists(fileLocation.getParent())) {
            Files.createDirectories(fileLocation.getParent());
        }
    }

}
