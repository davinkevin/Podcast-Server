package lan.dk.podcastserver.business;

import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.repository.CoverRepository;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.transaction.Transactional;

/**
 * Created by kevin on 08/06/2014.
 */
@Component
@Transactional
public class CoverBusiness {

    @Resource
    CoverRepository coverRepository;

    public Cover findOne(Integer integer) {
        return coverRepository.findOne(integer);
    }

    public boolean exists(Integer integer) {
        return coverRepository.exists(integer);
    }

    public Cover save(Cover cover) {
        return coverRepository.save(cover);
    }

    public Cover findByUrl(String url) {
        return coverRepository.findByUrl(url);
    }

    public Cover reatachCover(Cover cover) {
        Cover coverByUrl = findByUrl(cover.getUrl());

        if (coverByUrl != null) {
            return coverByUrl;
        }

        if (cover.getId() != null) {
            return findOne(cover.getId());
        }

        return cover;
    }
}
