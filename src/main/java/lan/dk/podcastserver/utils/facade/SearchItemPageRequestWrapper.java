package lan.dk.podcastserver.utils.facade;

import lan.dk.podcastserver.entity.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 08/06/2014 for Podcast Server
 */
public class SearchItemPageRequestWrapper extends PageRequestFacade {

    private List<Tag> tags = new ArrayList<>();
    private String term;
    private Boolean downloaded;

    public SearchItemPageRequestWrapper() {
        this.orders.add(new OrderFacade(Sort.Direction.DESC.toString(), "pubdate"));
        this.downloaded = Boolean.TRUE;
    }

    public List<Tag> getTags() {
        return tags;
    }
    public SearchItemPageRequestWrapper setTags(List<Tag> tags) {
        this.tags = tags;
        return this;
    }

    public String getTerm() {
        return term;
    }
    public SearchItemPageRequestWrapper setTerm(String term) {
        this.term = term;
        return this;
    }

    public SearchItemPageRequestWrapper downloaded(Boolean downloaded) {
        this.downloaded = downloaded;
        return this;
    }

    public Boolean getDownloaded() {
        return downloaded;
    }

    public Boolean isSearch() {
        return !StringUtils.isEmpty(term) || !tags.isEmpty() || nonNull(downloaded);
    }
}
