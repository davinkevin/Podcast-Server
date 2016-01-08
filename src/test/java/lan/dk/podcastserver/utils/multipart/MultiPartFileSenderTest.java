package lan.dk.podcastserver.utils.multipart;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 07/01/2016 for Podcast Server
 */
public class MultiPartFileSenderTest {

    public static final String STRING_FILE_PATH = "/__files/utils/multipart/file_to_serve.txt";
    HttpServletRequest request;
    HttpServletResponse response;
    Path filePath;

    @Before
    public void beforeEach() throws URISyntaxException {
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filePath = Paths.get(MultiPartFileSenderTest.class.getResource(STRING_FILE_PATH).toURI());
    }

    @Test
    public void should_be_config_with_path() throws URISyntaxException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        /* Given */
        /* When */
        MultiPartFileSender multiPartFileSender = MultiPartFileSender.fromPath(filePath);

        /* Then */
        assertThat(multiPartFileSender).isInstanceOf(MultiPartFileSender.class);
        assertThat(getField("filepath", Path.class, multiPartFileSender).toString()).contains(STRING_FILE_PATH);
    }

    @Test
    public void should_be_config_with_File() throws URISyntaxException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        /* Given */
        File filePath = Paths.get(MultiPartFileSenderTest.class.getResource(STRING_FILE_PATH).toURI()).toFile();

        /* When */
        MultiPartFileSender multiPartFileSender = MultiPartFileSender.fromFile(filePath);

        /* Then */
        assertThat(multiPartFileSender).isInstanceOf(MultiPartFileSender.class);
        assertThat(getField("filepath", Path.class, multiPartFileSender).toString()).contains(STRING_FILE_PATH);
    }

    @Test
    public void should_be_config_with_String_URI() throws URISyntaxException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        /* Given */
        /* When */
        MultiPartFileSender multiPartFileSender = MultiPartFileSender.fromURIString(STRING_FILE_PATH);

        /* Then */
        assertThat(multiPartFileSender).isInstanceOf(MultiPartFileSender.class);
        assertThat(getField("filepath", Path.class, multiPartFileSender).toString()).contains(STRING_FILE_PATH);
    }

    @Test
    public void should_be_config_with_request_and_response_and_return_inline() throws NoSuchFieldException, IllegalAccessException {
        /* Given */
        /* When */
        MultiPartFileSender multiPartFileSender = MultiPartFileSender
                .fromPath(filePath)
                .with(request)
                .with(response)
                .withDispositionInline();

        /* Then */
        assertThat(getField("request", HttpServletRequest.class, multiPartFileSender)).isSameAs(request);
        assertThat(getField("response", HttpServletResponse.class, multiPartFileSender)).isSameAs(response);
        assertThat(getField("disposition", String.class, multiPartFileSender)).isEqualTo("inline");
    }

    @Test
    public void should_be_config_with_disposition_attachment() throws NoSuchFieldException, IllegalAccessException {
        /* Given */
        /* When */
        MultiPartFileSender multiPartFileSender = MultiPartFileSender
                .fromPath(filePath)
                .with(request)
                .with(response)
                .withDispositionAttachment();

        /* Then */
        assertThat(getField("disposition", String.class, multiPartFileSender)).isEqualTo("attachment");
    }

    @Test
    public void should_be_config_with_no_disposition() throws NoSuchFieldException, IllegalAccessException {
        /* Given */
        /* When */
        MultiPartFileSender multiPartFileSender = MultiPartFileSender
                .fromPath(filePath)
                .with(request)
                .with(response)
                .withNoDisposition();

        /* Then */
        assertThat(getField("disposition", String.class, multiPartFileSender)).isNull();
    }

    @Test
    public void should_exit_if_response_or_request_are_null() throws Exception {
        /* Given */
        /* When */
        MultiPartFileSender
                .fromPath(filePath)
                .serveResource();
    }

    @Test
    public void should_return_error_if_file_doesnt_exist() throws Exception {
        /* Given */
        Path path = Paths.get("/__files/utils/multipart/doesntexists.txt");

        /* When */
        MultiPartFileSender
                .fromPath(path)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(response, only()).sendError(eq(HttpServletResponse.SC_NOT_FOUND));
    }

    @After
    public void afterEach() {
        verifyNoMoreInteractions(request, response);
    }

    @SuppressWarnings("unchecked")
    static <T, U> U getField(String name, Class<U> clazz, T bean) throws NoSuchFieldException, IllegalAccessException {
        Field field = bean.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return clazz.cast(field.get(bean));
    }

}