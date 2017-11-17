package lan.dk.podcastserver.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.file.Path;

import static io.vavr.API.Option;

/**
 * Created by kevin on 01/09/2017
 */
@Slf4j
@Component
public class ByteRangeResourceHandler extends ResourceHttpRequestHandler {

    public final static String ATTR_FILE = ByteRangeResourceHandler.class.getName() + ".file";

    @Override
    protected Resource getResource(HttpServletRequest request) throws IOException {
        return Option(request.getAttribute(ATTR_FILE))
                .map(Path.class::cast)
                .map(Path::toFile)
                .map(FileSystemResource::new)
                .getOrElseThrow(() -> new RuntimeException("Error during serving of byte range resources"));
    }
}
