package lan.dk.podcastserver.utils.facade;

import lan.dk.podcastserver.entity.Tag;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by kevin on 08/06/2014.
 */
public class SearchItemPageRequestWrapper extends AbstractPageRequestFacade {

    private List<Tag> tags = new ArrayList<>();
    private String term;

    public SearchItemPageRequestWrapper() {
        setProperties("pubdate");
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

    public Boolean isSearch() {
        return !StringUtils.isEmpty(term) || !tags.isEmpty();
    }
}
