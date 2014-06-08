package lan.dk.podcastserver.utils.facade;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

public class PageRequestFacade {

    //@RequestParam(value = "page", required = false, defaultValue = "0")
    private int page = 0;
    //@RequestParam(value = "size", required = false, defaultValue = "10")
    private Integer size = 10;
    //@RequestParam(value = "direction", required = false, defaultValue = "ASC")
    private String direction = "DESC";
    //@RequestParam(value = "properties", required = false)
    private String properties = null;

    public PageRequestFacade(Integer page, Integer size, String direction, String properties) {
        this.page = page;
        this.size = size;
        this.direction = direction;
        this.properties = properties;
    }

    public PageRequestFacade(Map<String,String> allRequestParams) {

    }

    public PageRequestFacade() {
    }

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

    @Override
    public String toString() {
        return "PageRequestFacade{" +
                "page=" + page +
                ", size=" + size +
                ", direction='" + direction + '\'' +
                ", properties='" + properties + '\'' +
                '}';
    }
}
