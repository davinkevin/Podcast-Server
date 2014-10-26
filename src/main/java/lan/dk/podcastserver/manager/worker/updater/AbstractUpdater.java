package lan.dk.podcastserver.manager.worker.updater;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.Validator;

//@Component
//@Scope("prototype")
@Transactional(noRollbackFor=Exception.class)
public abstract class AbstractUpdater implements Updater {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    protected final String USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.6; rv:25.0) Gecko/20100101 Firefox/25.0";

    @Value("${serverURL:http://localhost:8080}") private String serverURL;
    @Resource(name="Validator") Validator validator;

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

}
