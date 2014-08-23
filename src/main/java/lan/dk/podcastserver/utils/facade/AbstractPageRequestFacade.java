package lan.dk.podcastserver.utils.facade;

import org.springframework.data.domain.PageRequest;

/**
 * Created by kevin on 23/08/2014.
 */
public abstract class AbstractPageRequestFacade {

    private PageRequestFacade pageRequestFacade = new PageRequestFacade();

    public Integer getPage() {
        return pageRequestFacade.getPage();
    }

    public Integer getSize() {
        return pageRequestFacade.getSize();
    }

    public String getProperties() {
        return pageRequestFacade.getProperties();
    }

    public PageRequestFacade setProperties(String properties) {
        return pageRequestFacade.setProperties(properties);
    }

    public String getDirection() {
        return pageRequestFacade.getDirection();
    }

    public PageRequestFacade setSize(Integer size) {
        return pageRequestFacade.setSize(size);
    }

    public PageRequest toPageRequest() {
        return pageRequestFacade.toPageRequest();
    }

    public PageRequestFacade setDirection(String direction) {
        return pageRequestFacade.setDirection(direction);
    }

    public PageRequestFacade setPage(Integer page) {
        return pageRequestFacade.setPage(page);
    }
}
