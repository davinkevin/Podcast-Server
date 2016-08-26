package lan.dk.podcastserver.business;

import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.service.MimeTypeService;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@Transactional
@RequiredArgsConstructor
public class PodcastBusiness {

    final PodcastServerParameters podcastServerParameters;
    final JdomService jdomService;
    final PodcastRepository podcastRepository;
    final TagBusiness tagBusiness;
    final CoverBusiness coverBusiness;
    final MimeTypeService mimeTypeService;

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

        /*
        // Move folder if name has change :
        if (StringUtils.equals(podcastToUpdate.getTitle(), patchPodcast.getTitle())) {
                TODO : Move Folder to new Location using java.nio.FILES and java.nio.PATH
                It must add modification on each item of the podcast (localUrl)

        }
        */

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
                .setTags(tagBusiness.getTagListByName(patchPodcast.getTags()))
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
        try {
            return jdomService.podcastToXMLGeneric(findOne(id), limit, domainName);
        } catch (IOException e) {
            log.error("Unable to generate RSS for podcast {} with limit {}", id, limit, e);
            return "";
        }
    }

    public Set<Item> getItems(UUID id){
        return podcastRepository.findOne(id).getItems();
    }

    public Podcast reatachAndSave(Podcast podcast) {
        podcast.setTags(tagBusiness.getTagListByName(podcast.getTags()));
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
