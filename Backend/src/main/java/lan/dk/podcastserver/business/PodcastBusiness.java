package lan.dk.podcastserver.business;

import javaslang.collection.HashSet;
import javaslang.collection.Set;
import javaslang.control.Option;
import javaslang.control.Try;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class PodcastBusiness {

    private final PodcastServerParameters podcastServerParameters;
    private final JdomService jdomService;
    private final PodcastRepository podcastRepository;
    private final TagBusiness tagBusiness;
    private final CoverBusiness coverBusiness;

    //** Delegate du Repository **//
    public List<Podcast> findAll() {
        return podcastRepository.findAll();
    }

    public Podcast save(Podcast entity) {
        return podcastRepository.save(entity);
    }

    public Podcast findOne(UUID id) {
        return Option.of(podcastRepository.findOne(id)).getOrElseThrow(() -> new PodcastNotFoundException(id));
    }

    public void delete(UUID id) {
        podcastRepository.delete(id);
        //TODO : Delete the folder with java.nio.PATH and java.nio.FILES
    }

    public void delete(Podcast entity) {
        podcastRepository.delete(entity);
        //TODO : Delete the folder with java.nio.PATH and java.nio.FILES
    }

    public Set<Podcast> findByUrlIsNotNull() {
        return podcastRepository.findByUrlIsNotNull();
    }

    //*****//
    public Podcast patchUpdate(Podcast patchPodcast) {
        Podcast podcastToUpdate = this.findOne(patchPodcast.getId());

        // Move folder if name has change :
        if (!StringUtils.equals(podcastToUpdate.getTitle(), patchPodcast.getTitle())) {
            Try.of(() -> Files.move(
                    podcastServerParameters.getRootfolder().resolve(podcastToUpdate.getTitle()),
                    podcastServerParameters.getRootfolder().resolve(patchPodcast.getTitle())
            )).getOrElseThrow(e -> new UncheckedIOException(IOException.class.cast(e)));
        }

        if (!coverBusiness.hasSameCoverURL(patchPodcast, podcastToUpdate)) {
            patchPodcast.getCover().setUrl(coverBusiness.download(patchPodcast));
        }

        podcastToUpdate
                .setTitle(patchPodcast.getTitle())
                .setUrl(patchPodcast.getUrl())
                .setSignature(patchPodcast.getSignature())
                .setType(patchPodcast.getType())
                .setDescription(patchPodcast.getDescription())
                .setHasToBeDeleted(patchPodcast.getHasToBeDeleted())
                .setTags(tagBusiness.getTagListByName(HashSet.ofAll(patchPodcast.getTags())).toJavaSet())
                .setCover(
                    coverBusiness.findOne(patchPodcast.getCover().getId())
                        .setHeight(patchPodcast.getCover().getHeight())
                        .setUrl(patchPodcast.getCover().getUrl())
                        .setWidth(patchPodcast.getCover().getWidth())
                );


        return save(podcastToUpdate);
    }

    @Transactional(readOnly = true)
    public String getRss(UUID id, Boolean limit, String domainName) {
        return Try.of(() -> jdomService.podcastToXMLGeneric(findOne(id), domainName, limit))
            .onFailure(e -> log.error("Unable to generate RSS for podcast {} with limit {}", id, limit, e))
            .getOrElse(StringUtils.EMPTY);
    }

    public Podcast reatachAndSave(Podcast podcast) {
        podcast.setTags(tagBusiness.getTagListByName(HashSet.ofAll(podcast.getTags())).toJavaSet());
        return save(podcast);
    }
    
    public Podcast create(Podcast podcast) {
        Podcast podcastSaved = reatachAndSave(podcast);

        if (!Objects.isNull(podcast.getCover())) {
            coverBusiness.save(podcast.getCover().setUrl(coverBusiness.download(podcast)));
        }

        return podcastSaved;
    }

    public Path coverOf(UUID id) {
        return coverBusiness.getCoverPathOf(findOne(id));
    }
}
