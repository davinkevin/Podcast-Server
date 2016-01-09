package lan.dk.podcastserver.service;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static lan.dk.podcastserver.service.MultiPartFileSenderService.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 07/01/2016 for Podcast Server
 */
public class MultiPartFileSenderServiceTest {

    public static final String STRING_FILE_PATH = "/__files/utils/multipart/file_to_serve.txt";
    MultiPartFileSenderService multiPartFileSenderService;
    MimeTypeService mimeTypeService;
    HttpServletResponse response;
    HttpServletRequest request;
    Long lastModifiedDate;
    String fileName;
    Path binaryPath;
    Path filePath;
    Long length;

    @Before
    public void beforeEach() throws URISyntaxException, IOException {
        mimeTypeService = mock(MimeTypeService.class);
        multiPartFileSenderService = new MultiPartFileSenderService(mimeTypeService);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        binaryPath = Paths.get(MultiPartFileSenderServiceTest.class.getResource("/__files/utils/multipart/outputfile.out").toURI());
        filePath = Paths.get(MultiPartFileSenderServiceTest.class.getResource(STRING_FILE_PATH).toURI());
        lastModifiedDate = lastModifiedDate(filePath);
        length = Files.size(filePath);
        fileName = filePath.getFileName().toString();
    }

    @Test
    public void should_be_config_with_path() throws Exception {
        /* Given */
        /* When */
        MultiPartFileSenderImpl multiPartFileSender = multiPartFileSenderService.fromPath(filePath);
        multiPartFileSender.serveResource();

        /* Then */
        assertThat(multiPartFileSender).isInstanceOf(MultiPartFileSenderImpl.class);
        assertThat(getField("filepath", Path.class, multiPartFileSender).toString()).contains(STRING_FILE_PATH);
    }

    @Test
    public void should_be_config_with_File() throws URISyntaxException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        /* Given */
        File filePath = Paths.get(MultiPartFileSenderServiceTest.class.getResource(STRING_FILE_PATH).toURI()).toFile();

        /* When */
        MultiPartFileSenderImpl multiPartFileSender = multiPartFileSenderService.fromFile(filePath);

        /* Then */
        assertThat(multiPartFileSender).isInstanceOf(MultiPartFileSenderImpl.class);
        assertThat(getField("filepath", Path.class, multiPartFileSender).toString()).contains(STRING_FILE_PATH);
    }

    @Test
    public void should_be_config_with_String_URI() throws URISyntaxException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, NoSuchFieldException {
        /* Given */
        /* When */
        MultiPartFileSenderImpl multiPartFileSender = multiPartFileSenderService.fromURIString(STRING_FILE_PATH);

        /* Then */
        assertThat(multiPartFileSender).isInstanceOf(MultiPartFileSenderImpl.class);
        assertThat(getField("filepath", Path.class, multiPartFileSender).toString()).contains(STRING_FILE_PATH);
    }

    @Test
    public void should_be_config_with_request_and_response_and_return_inline() throws NoSuchFieldException, IllegalAccessException {
        /* Given */
        /* When */
        MultiPartFileSenderImpl multiPartFileSender = multiPartFileSenderService.
                fromPath(filePath)
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
        MultiPartFileSenderImpl multiPartFileSender = multiPartFileSenderService.
                fromPath(filePath)
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
        MultiPartFileSenderImpl multiPartFileSender = multiPartFileSenderService.
                fromPath(filePath)
                .with(request)
                .with(response)
                .withNoDisposition();

        /* Then */
        assertThat(getField("disposition", String.class, multiPartFileSender)).isNull();
    }

    @Test
    public void should_return_error_if_file_doesnt_exist() throws Exception {
        /* Given */
        Path path = Paths.get("/__files/utils/multipart/doesntexists.txt");

        /* When */
        multiPartFileSenderService.fromPath(path)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(response, only()).sendError(eq(HttpServletResponse.SC_NOT_FOUND));
    }

    @Test
    public void should_return_304_if_none_match_start() throws Exception {
        /* Given */
        when(request.getHeader(eq("If-None-Match"))).thenReturn("*");

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, only()).getHeader(eq("If-None-Match"));
        verify(response, times(1)).setHeader(eq("ETag"), eq("file_to_serve.txt"));
        verify(response, times(1)).sendError(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    public void should_return_not_modified() throws Exception {
        /* Given */
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(lastModifiedDate);

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(response, times(1)).setHeader(eq("ETag"), eq("file_to_serve.txt"));
        verify(response, times(1)).sendError(HttpServletResponse.SC_NOT_MODIFIED);
    }

    @Test
    public void should_return_precondition_failed() throws Exception {
        /* Given */
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("If-Match"))).thenReturn("foo");

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(response, times(1)).sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
    }

    @Test
    public void should_return_precondition_failed_after_unmodified_since() throws Exception {
        /* Given */
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(0L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(response, times(1)).sendError(HttpServletResponse.SC_PRECONDITION_FAILED);
    }

    @Test
    public void should_return_error_on_requested_range_not_satisfiable() throws Exception {
        /* Given */
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("foo");

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));

        verify(response, times(1)).setHeader(eq("Content-Range"), eq("bytes */" + length));
        verify(response, times(1)).sendError(eq(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE));
    }

    @Test
    public void should_return_full_file_with_no_disposition() throws Exception {
        /* Given */
        when(mimeTypeService.probeContentType(any(Path.class))).thenReturn("text/plain");
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=0-10");
        when(request.getHeader(eq("If-Range"))).thenReturn("not_file_to_serve.txt");
        when(request.getDateHeader(eq("If-Range"))).thenReturn(0L);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));
        verify(request, times(1)).getHeader(eq("If-Range"));
        verify(request, times(1)).getDateHeader(eq("If-Range"));
        verify(request, times(1)).getHeader(eq("Accept"));

        verify(response, times(1)).reset();
        verify(response, times(1)).setBufferSize(eq(20480));
        verify(response, times(1)).setHeader(eq("Content-Type"), eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Accept-Ranges"), eq("bytes"));
        verify(response, times(1)).setHeader(eq("ETag"), eq(fileName));
        verify(response, times(1)).setDateHeader(eq("Last-Modified"), eq(lastModifiedDate));
        verify(response, times(1)).setDateHeader(eq("Expires"), anyLong());
        verify(response, times(1)).setHeader(eq("Content-Disposition"), eq("attachment;filename=\"" + fileName + "\""));
        verify(response, times(1)).getOutputStream();
        verify(response, times(1)).setContentType(eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Content-Range"), anyString());
        verify(response, times(1)).setHeader(eq("Content-Length"), anyString());
    }


    @Test
    public void should_return_full_file_with_no_disposition_and_illegal_date_if_range_header() throws Exception {
        /* Given */
        when(mimeTypeService.probeContentType(any(Path.class))).thenReturn("text/plain");
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=0-10");
        when(request.getHeader(eq("If-Range"))).thenReturn("not_file_to_serve.txt");
        doThrow(IllegalArgumentException.class).when(request).getDateHeader(eq("If-Range"));
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));
        verify(request, times(1)).getHeader(eq("If-Range"));
        verify(request, times(1)).getDateHeader(eq("If-Range"));
        verify(request, times(1)).getHeader(eq("Accept"));

        verify(response, times(1)).reset();
        verify(response, times(1)).setBufferSize(eq(20480));
        verify(response, times(1)).setHeader(eq("Content-Type"), eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Accept-Ranges"), eq("bytes"));
        verify(response, times(1)).setHeader(eq("ETag"), eq(fileName));
        verify(response, times(1)).setDateHeader(eq("Last-Modified"), eq(lastModifiedDate));
        verify(response, times(1)).setDateHeader(eq("Expires"), anyLong());
        verify(response, times(1)).setHeader(eq("Content-Disposition"), eq("attachment;filename=\"" + fileName + "\""));
        verify(response, times(1)).getOutputStream();
        verify(response, times(1)).setContentType(eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Content-Range"), anyString());
        verify(response, times(1)).setHeader(eq("Content-Length"), anyString());
    }

    //@Ignore
    @Test
    public void should_return_part_file_with_no_disposition() throws Exception {
        /* Given */
        when(mimeTypeService.probeContentType(any(Path.class))).thenReturn("text/plain");
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=0-10");
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));
        verify(request, times(1)).getHeader(eq("If-Range"));
        verify(request, times(1)).getHeader(eq("Accept"));

        verify(response, times(1)).reset();
        verify(response, times(1)).setBufferSize(eq(20480));
        verify(response, times(1)).setHeader(eq("Content-Type"), eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Accept-Ranges"), eq("bytes"));
        verify(response, times(1)).setHeader(eq("ETag"), eq(fileName));
        verify(response, times(1)).setDateHeader(eq("Last-Modified"), eq(lastModifiedDate));
        verify(response, times(1)).setDateHeader(eq("Expires"), anyLong());
        verify(response, times(1)).setHeader(eq("Content-Disposition"), eq("attachment;filename=\"" + fileName + "\""));
        verify(response, times(1)).getOutputStream();
        verify(response, times(1)).setContentType(eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Content-Range"), anyString());
        verify(response, times(1)).setHeader(eq("Content-Length"), anyString());
        verify(response, times(1)).setStatus(eq(HttpServletResponse.SC_PARTIAL_CONTENT));
    }

    @Test
    public void should_return_reject_because_range_invalid() throws Exception {
        /* Given */
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=50-10");
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));
        verify(request, times(1)).getHeader(eq("If-Range"));

        verify(response, times(1)).setHeader(eq("Content-Range"), eq("bytes */" + length));
        verify(response, times(1)).sendError(eq(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE));
    }

    @Test
    public void should_return_part_file_with_no_disposition_between_star_and_10() throws Exception {
        /* Given */
        when(mimeTypeService.probeContentType(any(Path.class))).thenReturn("text/plain");
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=-10");
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));
        verify(request, times(1)).getHeader(eq("If-Range"));
        verify(request, times(1)).getHeader(eq("Accept"));

        verify(response, times(1)).reset();
        verify(response, times(1)).setBufferSize(eq(20480));
        verify(response, times(1)).setHeader(eq("Content-Type"), eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Accept-Ranges"), eq("bytes"));
        verify(response, times(1)).setHeader(eq("ETag"), eq(fileName));
        verify(response, times(1)).setDateHeader(eq("Last-Modified"), eq(lastModifiedDate));
        verify(response, times(1)).setDateHeader(eq("Expires"), anyLong());
        verify(response, times(1)).setHeader(eq("Content-Disposition"), eq("attachment;filename=\"" + fileName + "\""));
        verify(response, times(1)).getOutputStream();
        verify(response, times(1)).setContentType(eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Content-Range"), anyString());
        verify(response, times(1)).setHeader(eq("Content-Length"), anyString());
        verify(response, times(1)).setStatus(eq(HttpServletResponse.SC_PARTIAL_CONTENT));
    }

    @Test
    public void should_return_part_file_with_no_disposition_between_10_and_end() throws Exception {
        /* Given */
        when(mimeTypeService.probeContentType(any(Path.class))).thenReturn("text/plain");
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=10-");
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));
        verify(request, times(1)).getHeader(eq("If-Range"));
        verify(request, times(1)).getHeader(eq("Accept"));

        verify(response, times(1)).reset();
        verify(response, times(1)).setBufferSize(eq(20480));
        verify(response, times(1)).setHeader(eq("Content-Type"), eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Accept-Ranges"), eq("bytes"));
        verify(response, times(1)).setHeader(eq("ETag"), eq(fileName));
        verify(response, times(1)).setDateHeader(eq("Last-Modified"), eq(lastModifiedDate));
        verify(response, times(1)).setDateHeader(eq("Expires"), anyLong());
        verify(response, times(1)).setHeader(eq("Content-Disposition"), eq("attachment;filename=\"" + fileName + "\""));
        verify(response, times(1)).getOutputStream();
        verify(response, times(1)).setContentType(eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Content-Range"), anyString());
        verify(response, times(1)).setHeader(eq("Content-Length"), anyString());
        verify(response, times(1)).setStatus(eq(HttpServletResponse.SC_PARTIAL_CONTENT));
    }

    @Test
    public void should_return_full_file_with_binary_file_mimetype() throws Exception {
        /* Given */
        when(mimeTypeService.probeContentType(any(Path.class))).thenReturn("application/octet-stream");
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=0-10");
        when(request.getHeader(eq("If-Range"))).thenReturn("not_file_to_serve.txt");
        when(request.getHeader(eq("Accept"))).thenReturn("*/*");
        when(request.getDateHeader(eq("If-Range"))).thenReturn(0L);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        /* When */
        multiPartFileSenderService.fromPath(binaryPath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));
        verify(request, times(1)).getHeader(eq("If-Range"));
        verify(request, times(1)).getDateHeader(eq("If-Range"));
        verify(request, times(1)).getHeader(eq("Accept"));

        verify(response, times(1)).reset();
        verify(response, times(1)).setBufferSize(eq(20480));
        verify(response, times(1)).setHeader(eq("Content-Type"), eq("application/octet-stream"));
        verify(response, times(1)).setHeader(eq("Accept-Ranges"), eq("bytes"));
        verify(response, times(1)).setHeader(eq("ETag"), eq(binaryPath.getFileName().toString()));
        verify(response, times(1)).setDateHeader(eq("Last-Modified"), eq(lastModifiedDate(binaryPath)));
        verify(response, times(1)).setDateHeader(eq("Expires"), anyLong());
        verify(response, times(1)).setHeader(eq("Content-Disposition"), eq("inline;filename=\"" + binaryPath.getFileName().toString() + "\""));
        verify(response, times(1)).getOutputStream();
        verify(response, times(1)).setContentType(eq("application/octet-stream"));
        verify(response, times(1)).setHeader(eq("Content-Range"), anyString());
        verify(response, times(1)).setHeader(eq("Content-Length"), anyString());
    }

    @Test
    public void should_return_full_file_with_binary_if_mimeType_not_found() throws Exception {
        /* Given */
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=0-10");
        when(request.getHeader(eq("If-Range"))).thenReturn("not_file_to_serve.txt");
        when(request.getHeader(eq("Accept"))).thenReturn("*/*");
        when(request.getDateHeader(eq("If-Range"))).thenReturn(0L);
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        /* When */
        multiPartFileSenderService.fromPath(binaryPath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));
        verify(request, times(1)).getHeader(eq("If-Range"));
        verify(request, times(1)).getDateHeader(eq("If-Range"));
        //verify(request, times(1)).getHeader(eq("Accept"));

        verify(response, times(1)).reset();
        verify(response, times(1)).setBufferSize(eq(20480));
        verify(response, times(1)).setHeader(eq("Content-Type"), eq(null));
        verify(response, times(1)).setHeader(eq("Accept-Ranges"), eq("bytes"));
        verify(response, times(1)).setHeader(eq("ETag"), eq(binaryPath.getFileName().toString()));
        verify(response, times(1)).setDateHeader(eq("Last-Modified"), eq(lastModifiedDate(binaryPath)));
        verify(response, times(1)).setDateHeader(eq("Expires"), anyLong());
        verify(response, times(1)).setHeader(eq("Content-Disposition"), eq("inline;filename=\"" + binaryPath.getFileName().toString() + "\""));
        verify(response, times(1)).getOutputStream();
        verify(response, times(1)).setContentType(eq("application/octet-stream"));
        verify(response, times(1)).setHeader(eq("Content-Range"), anyString());
        verify(response, times(1)).setHeader(eq("Content-Length"), anyString());
    }


    @Test
    public void should_return_multiple_part_file_with_no_disposition() throws Exception {
        /* Given */
        when(mimeTypeService.probeContentType(any(Path.class))).thenReturn("text/plain");
        when(request.getDateHeader(eq("If-Unmodified-Since"))).thenReturn(-1L);
        when(request.getDateHeader(eq("If-Modified-Since"))).thenReturn(-1L);
        when(request.getHeader(eq("Range"))).thenReturn("bytes=5-10,15-23");
        when(response.getOutputStream()).thenReturn(mock(ServletOutputStream.class));

        /* When */
        multiPartFileSenderService.fromPath(filePath)
                .with(request)
                .with(response)
                .serveResource();

        /* Then */
        verify(request, times(1)).getHeader(eq("If-None-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Modified-Since"));
        verify(request, times(1)).getHeader(eq("If-Match"));
        verify(request, times(1)).getDateHeader(eq("If-Unmodified-Since"));
        verify(request, times(1)).getHeader(eq("Range"));
        verify(request, times(1)).getHeader(eq("If-Range"));
        verify(request, times(1)).getHeader(eq("Accept"));

        verify(response, times(1)).reset();
        verify(response, times(1)).setBufferSize(eq(20480));
        verify(response, times(1)).setHeader(eq("Content-Type"), eq("text/plain"));
        verify(response, times(1)).setHeader(eq("Accept-Ranges"), eq("bytes"));
        verify(response, times(1)).setHeader(eq("ETag"), eq(fileName));
        verify(response, times(1)).setDateHeader(eq("Last-Modified"), eq(lastModifiedDate));
        verify(response, times(1)).setDateHeader(eq("Expires"), anyLong());
        verify(response, times(1)).setHeader(eq("Content-Disposition"), eq("attachment;filename=\"" + fileName + "\""));
        verify(response, times(1)).getOutputStream();
        verify(response, times(1)).setContentType(eq("multipart/byteranges; boundary=MULTIPART_BYTERANGES"));
        verify(response, times(1)).setStatus(eq(HttpServletResponse.SC_PARTIAL_CONTENT));
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

    private Long lastModifiedDate(Path path) throws IOException {
        return LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.of(ZoneOffset.systemDefault().getId())).toEpochSecond(ZoneOffset.UTC);
    }
}