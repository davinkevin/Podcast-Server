package lan.dk.podcastserver.utils.form;

import lombok.Data;

import java.util.UUID;

/**
 * Created by kevin on 30/01/15.
 */
@Data
public class MovingItemInQueueForm {
    private UUID id;
    private Integer position;
}
