package lan.dk.podcastserver.utils.facade;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Getter @Setter
@Accessors(chain = true)
public class PageRequestFacade {

    protected int page = 0;
    protected Integer size = 10;
    protected List<OrderFacade> orders = new ArrayList<>();

    public PageRequest toPageRequest() {
        orders.add(new PageRequestFacade.OrderFacade(Sort.Direction.DESC.toString(), "creationDate"));
        return new PageRequest(this.page, this.size, new Sort(orders.stream().map(Sort.Order.class::cast).collect(toList())));
    }

    public static class OrderFacade extends Sort.Order {
        @JsonCreator
        public OrderFacade(@JsonProperty("direction") String direction, @JsonProperty("property") String property) {
            super(Sort.Direction.fromString(direction), property);
        }
    }
}
