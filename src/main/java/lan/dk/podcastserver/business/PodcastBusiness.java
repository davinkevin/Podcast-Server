package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.exception.PodcastNotFoundException;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.utils.DateUtils;
import lan.dk.podcastserver.utils.MimeTypeUtils;
import lan.dk.podcastserver.utils.jDomUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * Created by kevin on 26/12/2013.
 */
@Component
@Transactional
public class PodcastBusiness {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Resource
    private PodcastRepository podcastRepository;

    @Value("${rootfolder}")
    private String rootfolder;

    @Value("${serverURL}")
    private String serveurURL;

    @Value("${fileContainer}")
    protected String fileContainer;

    //** Delegate du Repository **//
    public List<Podcast> findAll() {
        return podcastRepository.findAll();
    }

    public List<Podcast> findAll(Sort sort) {
        return podcastRepository.findAll(sort);
    }

    public Page<Podcast> findAll(Pageable pageable) {
        return podcastRepository.findAll(pageable);
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

    public Podcast generatePodcastFromURL(String URL) {
        logger.debug("URL = " + URL);
        try {
            return jDomUtils.getPodcastFromURL(new java.net.URL(URL));
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return null;
    }

    public Podcast update(Podcast podcast) {
        return podcastRepository.save(podcast);
    }

    @Transactional(readOnly = true)
    public String getRss(int id) {
        return podcastRepository.findOne(id).toXML(serveurURL);
    }

    @Transactional
    public boolean addItemByUpload(Integer idPodcast, MultipartFile file) throws PodcastNotFoundException, ParseException, IOException {
        Podcast podcast = podcastRepository.findOne(idPodcast);
        if (podcast == null) {
            throw new PodcastNotFoundException();
        }

        //TODO utiliser BEAN_UTIL pour faire du dynamique :
        // 1er temps : Template en dure : {title} - {date} - {title}.mp3

        Item item = new Item();
        String fileName = file.getOriginalFilename();
        File fileToSave = new File(rootfolder + File.separator + podcast.getTitle() + File.separator + fileName);
        if (fileToSave.exists()) {
            fileToSave.delete();
        }
        fileToSave.mkdirs();

        file.transferTo(fileToSave);

        item.setTitle(fileName.split(" - ")[2])
            .setPubdate(DateUtils.folderDateToTimestamp(fileName.split(" - ")[1]))
            .setUrl(null)
            .setLength(file.getSize())
            .setMimeType(MimeTypeUtils.getMimeType(FilenameUtils.getExtension(fileName)))
            .setDescription(podcast.getDescription())
            .setLocalUrl(fileContainer + "/" + podcast.getTitle() + "/" + fileName)
            .setLocalUri(fileToSave.getAbsolutePath())
            .setDownloaddate(new Timestamp(new Date().getTime()))
            .setPodcast(podcast)
            .setStatus("Finish");

        podcast.getItems().add(item);

        podcastRepository.save(podcast);
        return (item.getId() != 0);
    }
}
