package lan.dk.podcastserver.service;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;

public class DownloadManager implements Runnable {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private URL downloadURL;
    private File destinationFile;

    public DownloadManager(URL downloadURL, File destinationFile) {
        this.downloadURL = downloadURL;
        this.destinationFile = destinationFile;
    }

    public URL getDownloadURL() {
        return downloadURL;
    }

    public void setDownloadURL(URL downloadURL) {
        this.downloadURL = downloadURL;
    }

    public File getDestinationFile() {
        return destinationFile;
    }

    public void setDestinationFile(File destinationFile) {
        this.destinationFile = destinationFile;
    }

    public void run() {
        logger.info("Connecting to " + this.getDownloadURL().toString());
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(this.getDownloadURL().toString());
        HttpResponse response = null;
        try {
            response = client.execute(get);
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        InputStream input = null;
        OutputStream output = null;
        byte[] buffer = new byte[1024];
        try {
            logger.info("Downloading file to " + this.getDestinationFile().getName());
            input = response.getEntity().getContent();
            output = new FileOutputStream(this.getDestinationFile());
            for (int length; (length = input.read(buffer)) > 0;) {
                output.write(buffer, 0, length);
            }
            logger.info("File " + this.getDestinationFile().getName() + " successfully downloaded!");
        } catch (FileNotFoundException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } finally {
            if (output != null) try { output.close(); } catch (IOException logOrIgnore) {}
            if (input != null) try { input.close(); } catch (IOException logOrIgnore) {}
        }

    }
}
