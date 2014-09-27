package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.repository.ItemRepository;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.utils.DateUtils;
import lan.dk.podcastserver.utils.MimeTypeUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Set;

@Component
@Transactional
public class PodcastBusiness {

    @Resource private PodcastRepository podcastRepository;
    @Resource private ItemRepository itemRepository;
    @Resource private TagBusiness tagBusiness;
    @Resource private CoverBusiness coverBusiness;

    @Value("${rootfolder:${catalina.home}/webapp/podcast/}")
    private String rootfolder;

    @Value("${serverURL:http://localhost:8080}")
    private String serveurURL;

    @Value("${fileContainer:http://localhost:8080/podcast}")
    protected String fileContainer;

    public String getRootfolder() {
        return rootfolder;
    }

    public String getFileContainer() {
        return fileContainer;
    }

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
    }

    public void delete(Podcast entity) {
        podcastRepository.delete(entity);
    }

    public List<Podcast> findByUrlIsNotNull() {
        return podcastRepository.findByUrlIsNotNull();
    }

    //*****//
    public Podcast patchUpdate(Podcast patchPodcast) throws PodcastNotFoundException {
        Podcast podcastToUpdate = this.findOne(patchPodcast.getId());

        if (podcastToUpdate == null)
            throw new PodcastNotFoundException();

        podcastToUpdate.setTitle(patchPodcast.getTitle());
        podcastToUpdate.setUrl(patchPodcast.getUrl());
        podcastToUpdate.setSignature(patchPodcast.getSignature());
        podcastToUpdate.setType(patchPodcast.getType());

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
    public String getRss(int id) {
        return this.findOne(id).toXML(serveurURL);
    }

    @Transactional
    @Deprecated
    public boolean addItemByUpload(Integer idPodcast, MultipartFile file, String name) throws PodcastNotFoundException, ParseException, IOException {
        Podcast podcast = this.findOne(idPodcast);
        if (podcast == null) {
            throw new PodcastNotFoundException();
        }

        //TODO utiliser BEAN_UTIL pour faire du dynamique :
        // 1er temps : Template en dure : {title} - {date} - {title}.mp3

        Item item = new Item();
        //String name = name;
        File fileToSave = new File(rootfolder + File.separator + podcast.getTitle() + File.separator + name);
        if (fileToSave.exists()) {
            fileToSave.delete();
        }
        fileToSave.mkdirs();

        file.transferTo(fileToSave);

        item.setTitle(FilenameUtils.removeExtension(name.split(" - ")[2]))
            .setPubdate(DateUtils.fromFolder(name.split(" - ")[1]))
            .setUrl(fileContainer + "/" + podcast.getTitle() + "/" + name)
            .setLength(file.getSize())
            .setMimeType(MimeTypeUtils.getMimeType(FilenameUtils.getExtension(name)))
            .setDescription(podcast.getDescription())
            .setLocalUrl(fileContainer + "/" + podcast.getTitle() + "/" + name)
            .setLocalUri(fileToSave.getAbsolutePath())
            .setDownloaddate(ZonedDateTime.of(LocalDateTime.now(), ZoneId.systemDefault()))
            .setPodcast(podcast)
            .setStatus("Finish");

        podcast.getItems().add(item);

        itemRepository.save(item);
        this.save(podcast);

        return (item.getId() != 0);
    }


    public Set<Item> getItems(int id){
        return this.findOne(id).getItems();
    }

    public Podcast reatachAndSave(Podcast podcast) {

        Cover coverToSave = podcast.getCover();
        //podcast.setCover(coverBusiness.reatachCover(podcast.getCover()));
        podcast.setTags(tagBusiness.getTagListByName(podcast.getTags()));

        podcast.getCover()
            .setHeight(coverToSave.getHeight())
            .setWidth(coverToSave.getWidth())
            .setUrl(coverToSave.getUrl());

        return save(podcast);
    }
}
