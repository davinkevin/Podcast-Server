package lan.dk.podcastserver.utils.facade;

import lan.dk.podcastserver.entity.Tag;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * Created by kevin on 08/06/2014 for Podcast Server
 */
@Getter @Setter
@Accessors(chain = true)
public class SearchItemPageRequestWrapper extends PageRequestFacade {

    private List<Tag> tags = new ArrayList<>();
    private String term;
    private Boolean downloaded;

    public SearchItemPageRequestWrapper() {
        this.orders.add(new OrderFacade(Sort.Direction.DESC.toString(), "pubDate"));
        this.downloaded = Boolean.TRUE;
    }

    public Boolean isSearch() {
        return !StringUtils.isEmpty(term) || !tags.isEmpty() || nonNull(downloaded);
    }
}
