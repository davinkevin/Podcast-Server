package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.entity.Status;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.service.PodcastServerParameters;
import lan.dk.podcastserver.service.JdomService;
import lan.dk.podcastserver.utils.MimeTypeUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
@Transactional
public class PodcastBusiness {

    public static final String UPLOAD_PATTERN = "yyyy-MM-dd";

    @Resource private PodcastServerParameters podcastServerParameters;
    @Resource private JdomService jdomService;
    
    @Resource private PodcastRepository podcastRepository;
    
    @Resource private ItemBusiness itemBusiness;
    @Resource private TagBusiness tagBusiness;
    @Resource private CoverBusiness coverBusiness;


    //** Delegate du Repository **//
    public List<Podcast> findAll() {
        return podcastRepository.findAll();
    }

    public Podcast save(Podcast entity) {
        return podcastRepository.save(entity);
    }

    public Podcast findOne(Integer integer) {
        return podcastRepository.findOne(integer);
    }

    public void delete(Integer integer) {
        podcastRepository.delete(integer);
        //TODO : Delete the folder with java.nio.PATH and java.nio.FILES
    }

    public void delete(Podcast entity) {
        podcastRepository.delete(entity);
        //TODO : Delete the folder with java.nio.PATH and java.nio.FILES
    }

    public List<Podcast> findByUrlIsNotNull() {
        return podcastRepository.findByUrlIsNotNull();
    }

    //*****//
    public Podcast patchUpdate(Podcast patchPodcast) throws PodcastNotFoundException {
        Podcast podcastToUpdate = this.findOne(patchPodcast.getId());

        if (podcastToUpdate == null)
            throw new PodcastNotFoundException();

        // Move folder if name has change : 
        if (StringUtils.equals(podcastToUpdate.getTitle(), patchPodcast.getTitle())) {
            /* 
                TODO : Move Folder to new Location using java.nio.FILES and java.nio.PATH
                It must add modification on each item of the podcast (localUrl)
             */
            
        }
        
        podcastToUpdate.setTitle(patchPodcast.getTitle());
        podcastToUpdate.setUrl(patchPodcast.getUrl());
        podcastToUpdate.setSignature(patchPodcast.getSignature());
        podcastToUpdate.setType(patchPodcast.getType());

        if (!coverBusiness.hasSameCoverURL(patchPodcast, podcastToUpdate)) {
            patchPodcast.getCover().setUrl(coverBusiness.download(patchPodcast));
        }
        
        podcastToUpdate.setCover(
                coverBusiness.findOne(patchPodcast.getCover().getId())
                    .setHeight(patchPodcast.getCover().getHeight())
                    .setUrl(patchPodcast.getCover().getUrl())
                    .setWidth(patchPodcast.getCover().getWidth())
        );
        podcastToUpdate.setDescription(patchPodcast.getDescription());
        podcastToUpdate.setHasToBeDeleted(patchPodcast.getHasToBeDeleted());
        podcastToUpdate.setTags(patchPodcast.getTags());

        return this.reatachAndSave(podcastToUpdate);
    }

    public Podcast update(Podcast podcast) {
        return this.save(podcast);
    }

    @Transactional(readOnly = true)
    public String getRss(Integer id, Boolean limit) {
        return (limit) ? jdomService.podcastToXMLGeneric(findOne(id)) : jdomService.podcastToXMLGeneric(findOne(id), null);
    }

    @Transactional
    @Deprecated
    public boolean addItemByUpload(Integer idPodcast, MultipartFile file, String name) throws PodcastNotFoundException, ParseException, IOException, URISyntaxException {
        Podcast podcast = this.findOne(idPodcast);
        if (podcast == null) {
            throw new PodcastNotFoundException();
        }

        //TODO utiliser BEAN_UTIL pour faire du dynamique :
        // 1er temps : Template en dure : {title} - {date} - {title}.mp3

        Item item = new Item();
        //String name = name;
        /*File fileToSave = new File(rootfolder + File.separator + podcast.getTitle() + File.separator + name);*/
        File fileToSave = podcastServerParameters.rootFolder().resolve(podcast.getTitle()).resolve(name).toFile();
        if (fileToSave.exists()) {
            fileToSave.delete();
        }
        fileToSave.mkdirs();

        file.transferTo(fileToSave);

        item.setTitle(FilenameUtils.removeExtension(name.split(" - ")[2]))
            .setPubdate(fromFolder(name.split(" - ")[1]))
            .setUrl(UriComponentsBuilder.fromUri(podcastServerParameters.fileContainer()).pathSegment(podcast.getTitle()).pathSegment(name).build().toUriString())
            .setLength(file.getSize())
            .setMimeType(MimeTypeUtils.getMimeType(FilenameUtils.getExtension(name)))
            .setDescription(podcast.getDescription())
            .setFileName(name)
            .setDownloadDate(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()))
            .setPodcast(podcast)
            .setStatus(Status.FINISH);

        podcast.getItems().add(item);
        podcast.setLastUpdate(ZonedDateTime.now());

        itemBusiness.save(item);
        this.save(podcast);

        return (item.getId() != 0);
    }


    public Set<Item> getItems(int id){
        return this.findOne(id).getItems();
    }

    public Podcast reatachAndSave(Podcast podcast) {
        podcast.setTags(tagBusiness.getTagListByName(podcast.getTags()));
        return save(podcast);
    }
    
    public Podcast create(Podcast podcast) {
        if (!Objects.isNull(podcast.getCover())) {
            podcast.getCover().setUrl(coverBusiness.download(podcast));
        }
        
        return reatachAndSave(podcast);
    }

    public ZonedDateTime fromFolder(String pubDate) {
        return ZonedDateTime.of(LocalDateTime.of(LocalDate.parse(pubDate, DateTimeFormatter.ofPattern(UPLOAD_PATTERN)), LocalTime.of(0, 0)), ZoneId.systemDefault());
    }


}
