package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.utils.URLUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLConnection;

/**
 * Created by kevin on 28/02/15.
 */
@Scope("prototype")
@Component("DailyMotionCloudDownloader")
public class DailyMotionCloudDownloader extends M3U8Downloader {
    
    String redirectionUrl = null;

    @Override
    public String getItemUrl() {
        if (redirectionUrl == null) {

            String hlsStreamUrl = URLUtils.getRealURL(item.getUrl());

            BufferedReader in = null;
            try {
                URLConnection urlConnection = URLUtils.getStreamWithTimeOut(hlsStreamUrl);
                
                in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (inputLine.contains("audio") && inputLine.contains("video")) {
                        redirectionUrl = inputLine;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (in != null)
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
            }
            redirectionUrl = URLUtils.urlWithDomain(hlsStreamUrl, redirectionUrl);
        }
        
        return redirectionUrl;
    }
}
