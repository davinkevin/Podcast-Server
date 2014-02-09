package lan.dk.podcastserver.manager.worker.downloader;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.utils.URLUtils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

@Component("M3U8Downloader")
@Scope("prototype")
public class M3U8Downloader extends AbstractDownloader {

    List<String> urlList;
    OutputStream outputStream;
    Runnable runnableDownloader;


    @Override
    public Item download() {
        logger.debug("Download");
        itemDownloadManager.addACurrentDownload();

        if (item.getUrl().endsWith("m3u8")) {
            URL urlListFile = null;
            try {
                urlListFile = new URL(item.getUrl());
                urlList = new ArrayList<String>();

                BufferedReader in = new BufferedReader(new InputStreamReader(urlListFile.openStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    if (!inputLine.startsWith("#")) {
                        urlList.add(inputLine);
                    }
                }
                in.close();

                // Tous les éléments sont disponibles dans urlList :
                target = getTagetFile(item);

                outputStream = new FileOutputStream(target);
                runnableDownloader = new Runnable() {
                    @Override
                    public void run() {

                        try {
                            for (int cpt = 0; cpt < urlList.size(); cpt++) {
                                String urlFragmentToDownload = urlList.get(cpt);

                                InputStream is = new URL(urlFragmentToDownload).openStream();
                                int read = 0;
                                byte[] bytes = new byte[1024];

                                while ((read = is.read(bytes)) != -1) {
                                    outputStream.write(bytes, 0, read);
                                }
                                item.setProgression((100*cpt)/urlList.size());
                                logger.debug("Progression : {}", item.getProgression());
                                is.close();

                                if (stopDownloading.get()) {
                                    if (item.getStatus().equals("Stopped")) {
                                        break;
                                    } else if (item.getStatus().equals("Paused")) {

                                        synchronized(this)
                                        {
                                            this.wait();
                                        }
                                    }
                                }
                            }

                            if (item.getStatus().equals("Started")) {
                                finishDownload();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        } finally {
                            try {
                                outputStream.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };

                new Thread(runnableDownloader).start();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            logger.debug("Traiter avec le downloader adapté à {}", item.getUrl());
            stopDownload();
        }
        return this.item;
    }

    @Override
    public File getTagetFile (Item item) {
        File file = new File(itemDownloadManager.getRootfolder() + File.separator + item.getPodcast().getTitle() + File.separator + URLUtils.getFileNameFromCanalPlusM3U8Url(item.getUrl()) + temporaryExtension );
        logger.debug("Création du fichier : " + file.getAbsolutePath() );
        //logger.debug(file.getAbsolutePath());
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file;
    }

    @Override
    public void startDownload() {
        this.item.setStatus("Started");
        stopDownloading.set(false);
        this.saveSyncWithPodcast(this.item);

        if (runnableDownloader == null) {
            this.download();
        } else {
            synchronized (runnableDownloader) {
                runnableDownloader.notify();
            }
        }
    }
}
