package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.PodcastRepository;
import lan.dk.podcastserver.utils.jDomUtils;
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

import javax.annotation.Resource;
import java.net.MalformedURLException;
import java.net.URL;
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

    @Value("${serverURL}")
    private String serveurURL;


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
}
