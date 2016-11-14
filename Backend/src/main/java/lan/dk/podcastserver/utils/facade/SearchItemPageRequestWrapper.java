package lan.dk.podcastserver.utils.facade;

import javaslang.collection.List;
import lan.dk.podcastserver.entity.Tag;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;
import static java.util.Objects.nonNull;

/**
 * Created by kevin on 08/06/2014 for Podcast Server
 */
@EqualsAndHashCode(callSuper = true)
@Getter @Setter
@Accessors(chain = true)
public class SearchItemPageRequestWrapper extends PageRequestFacade {

    private List<Tag> tags = List.empty();
    private String term;
    private Boolean downloaded;

    public SearchItemPageRequestWrapper() {
        this.orders = this.orders.append(new OrderFacade(Sort.Direction.DESC.toString(), "pubDate"));
        this.downloaded = Boolean.TRUE;
    }

    public Boolean isSearch() {
        return !StringUtils.isEmpty(term) || !tags.isEmpty() || nonNull(downloaded);
    }
}
