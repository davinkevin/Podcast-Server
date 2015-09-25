package lan.dk.podcastserver.utils.facade;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public class PageRequestFacade {

    protected int page = 0;
    protected Integer size = 10;
    protected String direction = "DESC";
    protected String properties = null;

    public Integer getPage() {
        return page;
    }

    public PageRequestFacade setPage(Integer page) {
        this.page = page;
        return this;
    }

    public Integer getSize() {
        return size;
    }

    public PageRequestFacade setSize(Integer size) {
        this.size = size;
        return this;
    }

    public String getDirection() {
        return direction;
    }

    public PageRequestFacade setDirection(String direction) {
        this.direction = direction;
        return this;
    }

    public String getProperties() {
        return properties;
    }

    public PageRequestFacade setProperties(String properties) {
        this.properties = properties;
        return this;
    }

    public PageRequest toPageRequest() {
        return new PageRequest(this.page, this.size, Sort.Direction.fromString(this.direction), this.properties);
    }
}
