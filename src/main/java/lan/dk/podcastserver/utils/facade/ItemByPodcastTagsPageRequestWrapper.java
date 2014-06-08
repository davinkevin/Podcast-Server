package lan.dk.podcastserver.utils.facade;

import lan.dk.podcastserver.entity.Tag;
import org.springframework.data.domain.PageRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 08/06/2014.
 */
public class ItemByPodcastTagsPageRequestWrapper {

    private PageRequestFacade pageRequestFacade = new PageRequestFacade();
    private List<Tag> tags = new ArrayList<>();

    public PageRequestFacade setPage(Integer page) {
        return pageRequestFacade.setPage(page);
    }

    public Integer getSize() {
        return pageRequestFacade.getSize();
    }

    public PageRequestFacade setSize(Integer size) {
        return pageRequestFacade.setSize(size);
    }

    public String getDirection() {
        return pageRequestFacade.getDirection();
    }

    public PageRequestFacade setDirection(String direction) {
        return pageRequestFacade.setDirection(direction);
    }

    public String getProperties() {
        return pageRequestFacade.getProperties();
    }

    public PageRequestFacade setProperties(String properties) {
        return pageRequestFacade.setProperties(properties);
    }

    public PageRequest toPageRequest() {
        return pageRequestFacade.toPageRequest();
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}
