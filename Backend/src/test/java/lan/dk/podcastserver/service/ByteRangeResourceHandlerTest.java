package lan.dk.podcastserver.service;

import org.junit.Test;
import org.springframework.core.io.Resource;

import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 02/09/2017
 */
public class ByteRangeResourceHandlerTest {

    private ByteRangeResourceHandler brh = new ByteRangeResourceHandler();

    @Test
    public void should_expose_a_file_resource() throws IOException {
        /* GIVEN */
        Path file = Paths.get("/tmp/foo");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute(ByteRangeResourceHandler.ATTR_FILE)).thenReturn(file);

        /* WHEN  */
        Resource r = brh.getResource(request);

        /* THEN  */
        assertThat(r.getFile().toPath()).isEqualByComparingTo(file);
    }

    @Test
    public void should_raise_exception_if_no_attribute() {
        /* GIVEN */
        HttpServletRequest request = mock(HttpServletRequest.class);
        /* WHEN  */
        assertThatThrownBy(() -> brh.getResource(request))
        /* THEN  */
                .hasMessage("Error during serving of byte range resources");
    }

}