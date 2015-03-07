package lan.dk.podcastserver.manager.worker.selector;

import lan.dk.podcastserver.manager.worker.selector.update.UpdaterCompatibility;
import org.jadira.usertype.spi.utils.lang.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.Set;

/**
 * Created by kevin on 06/03/15.
 */
@Service
public class UpdaterSelector {
    
    Set<UpdaterCompatibility> updaterCompatibilities;
    
    public Class of(String url) {
        if (StringUtils.isEmpty(url)) {
            throw new RuntimeException();
        }
        
        return updaterCompatibilities
                .stream()
                .min(Comparator.comparing(updater -> updater.compatibility(url)))
                .get()
                .updater();
    }

    @Resource
    public UpdaterSelector setUpdaterCompatibilities(Set<UpdaterCompatibility> updaterCompatibilities) {
        this.updaterCompatibilities = updaterCompatibilities;
        return this;
    }
}
