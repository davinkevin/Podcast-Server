package com.gitlab.davinkevin.podcastserver.youtubedl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitlab.davinkevin.podcastserver.youtubedl.mapper.VideoFormat;
import com.gitlab.davinkevin.podcastserver.youtubedl.mapper.VideoInfo;
import com.gitlab.davinkevin.podcastserver.youtubedl.mapper.VideoThumbnail;
import com.gitlab.davinkevin.podcastserver.youtubedl.utils.StreamGobbler;
import com.gitlab.davinkevin.podcastserver.youtubedl.utils.StreamProcessExtractor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>Provide an interface for youtube-dl executable</p>
 *
 * <p>
 *     For more information on youtube-dl, please see
 *     <a href="https://github.com/rg3/youtube-dl/blob/master/README.md">YoutubeDL Documentation</a>
 * </p>
 */
public class YoutubeDL {

    /**
     * Youtube-dl executable name
     */
    private final String executablePath;
    private static final Pattern SPLIT_PATTERN = Pattern
            .compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");

    public YoutubeDL(String executablePath) {
        this.executablePath = executablePath;
    }

    public YoutubeDL() {
        this.executablePath = "youtube-dl";
    }

    /**
     * Append executable name to command
     * @param command Command string
     * @return Command string
     */
    protected String buildCommand(String command) {
        return String.format("%s %s", executablePath, command);
    }

    /**
     * Execute youtube-dl request
     * @param request request object
     * @return response object
     * @throws YoutubeDLException
     */
    public YoutubeDLResponse execute(YoutubeDLRequest request) throws YoutubeDLException {
        return execute(request, null);
    }
    
    private String[] splitCommand(String command) {
        final List<String> matchList = new ArrayList<>();
        final Matcher regexMatcher = SPLIT_PATTERN.matcher(command);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                matchList.add(regexMatcher.group(2));
            } else {
                matchList.add(regexMatcher.group());
            }
        }
        return matchList.toArray(new String[]{});
    }

    /**
     * Execute youtube-dl request
     * @param request request object
     * @param callback callback
     * @return response object
     * @throws YoutubeDLException
     */
    public YoutubeDLResponse execute(YoutubeDLRequest request, DownloadProgressCallback callback) throws YoutubeDLException {

        String command = buildCommand(request.buildOptions());
        String directory = request.getDirectory();
        Map<String, String> options = request.getOption();

        YoutubeDLResponse youtubeDLResponse;
        Process process;
        int exitCode;
        StringBuffer outBuffer = new StringBuffer(); //stdout
        StringBuffer errBuffer = new StringBuffer(); //stderr
        long startTime = System.nanoTime();

        String[] split = this.splitCommand(command);

        ProcessBuilder processBuilder = new ProcessBuilder(split);

        // Define directory if one is passed
        if(directory != null)
            processBuilder.directory(new File(directory));

        try {
            process = processBuilder.start();
        } catch (IOException e) {
            throw new YoutubeDLException(e);
        }

        InputStream outStream = process.getInputStream();
        InputStream errStream = process.getErrorStream();

        StreamProcessExtractor stdOutProcessor = new StreamProcessExtractor(outBuffer, outStream, callback);
        StreamGobbler stdErrProcessor = new StreamGobbler(errBuffer, errStream);

        try {
            stdOutProcessor.join();
            stdErrProcessor.join();
            exitCode = process.waitFor();
        } catch (InterruptedException e) {

            // process exited for some reason
            throw new YoutubeDLException(e);
        }

        String out = outBuffer.toString();
        String err = errBuffer.toString();

        if(exitCode > 0) {
            throw new YoutubeDLException(err);
        }

        int elapsedTime = (int) ((System.nanoTime() - startTime) / 1000000);

        youtubeDLResponse = new YoutubeDLResponse(command, options, directory, exitCode , elapsedTime, out, err);

        return youtubeDLResponse;
    }


    /**
     * Get youtube-dl executable version
     * @return version string
     * @throws YoutubeDLException
     */
    public String getVersion() throws YoutubeDLException {
        YoutubeDLRequest request = new YoutubeDLRequest();
        request.setOption("version");
        return this.execute(request).getOut();
    }

    /**
     * Retrieve all information available on a video
     * @param url Video url
     * @return Video info
     * @throws YoutubeDLException
     */
    public VideoInfo getVideoInfo(String url) throws YoutubeDLException  {

        // Build request
        YoutubeDLRequest request = new YoutubeDLRequest(url);
        request.setOption("dump-json");
        request.setOption("no-playlist");
        YoutubeDLResponse response = this.execute(request);

        // Parse result
        ObjectMapper objectMapper = new ObjectMapper();
        VideoInfo videoInfo;

        try {
            videoInfo = objectMapper.readValue(response.getOut(), VideoInfo.class);
        } catch (IOException e) {
            throw new YoutubeDLException("Unable to parse video information: " + e.getMessage());
        }

        return videoInfo;
    }

    /**
     * List formats
     * @param url Video url
     * @return list of formats
     * @throws YoutubeDLException
     */
    public List<VideoFormat> getFormats(String url) throws YoutubeDLException {
        VideoInfo info = this.getVideoInfo(url);
        return info.formats;
    }

    /**
     * List thumbnails
     * @param url Video url
     * @return list of thumbnail
     * @throws YoutubeDLException
     */
    public List<VideoThumbnail> getThumbnails(String url) throws YoutubeDLException {
        VideoInfo info = this.getVideoInfo(url);
        return info.thumbnails;
    }

    /**
     * List categories
     * @param url Video url
     * @return list of category
     * @throws YoutubeDLException
     */
    public List<String> getCategories(String url) throws YoutubeDLException {
        VideoInfo info = this.getVideoInfo(url);
        return info.categories;
    }

    /**
     * List tags
     * @param url Video url
     * @return list of tag
     * @throws YoutubeDLException
     */
    public List<String> getTags(String url) throws YoutubeDLException {
        VideoInfo info = this.getVideoInfo(url);
        return info.tags;
    }

    /**
     * Get command executable or path to the executable
     * @return path string
     */
    public String getExecutablePath(){
        return executablePath;
    }
}
