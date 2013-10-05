package lan.dk.podcastserver.service;

import org.jdom2.*;
import org.jdom2.input.DOMBuilder;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.util.List;


@Service
public class ScanRSSPodcastService {


    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${rootfolder}")
    protected String rootFolder;

    @Value("${serverURL}")
    protected String serverURL;

    //@Scheduled(fixedDelay = 600000)
    //@Scheduled(cron="0 0 * * * ?")
    public void process()
    {
        Document allPodcastDescriptionXML = null;

        try {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder;
            dBuilder = dbFactory.newDocumentBuilder();
            org.w3c.dom.Document doc = dBuilder.parse(new File(rootFolder + "index.xml"));
            DOMBuilder domBuilder = new DOMBuilder();
            allPodcastDescriptionXML = domBuilder.build(doc);
        } catch (ParserConfigurationException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (SAXException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        List<Element> listOfPodcastXml = allPodcastDescriptionXML.getRootElement().getChildren("podcast");

        for (Element podcast : listOfPodcastXml) {
            if (podcast.getAttributeValue("type") != null && podcast.getAttributeValue("type").equals("rss")) {
                logger.info("Traitement du Flux " + podcast.getChild("title").getValue());
                try {
                    transformRSS(podcast.getChild("title").getValue(), podcast.getChild("fluxexterne").getValue(), serverURL, 15);
                } catch (JDOMException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (IOException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                } catch (ParseException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
    }


    public void transformRSS (String nomDuPodcast, String urlDuPodcast, String urlDuServeurdePodcast, int maxSyncEpisodes) throws JDOMException, IOException, ParseException {

        // Creation of the Logger :
        File CurrentDirectory = new File(rootFolder + nomDuPodcast );
        SAXBuilder sax = new SAXBuilder();
        URL url = new URL(urlDuPodcast);
        Document doc = sax.build(url);

        Namespace feedburnerNS = Namespace.getNamespace("feedburner", "http://rssnamespace.org/feedburner/ext/1.0");
        XMLOutputter xout = new XMLOutputter(Format.getPrettyFormat());

        // Sauvegarde de la version originale :
        xout.output(doc, new FileOutputStream(CurrentDirectory.getAbsolutePath() + "/" + nomDuPodcast + "FromSite.xml"));

		/* Changement du titre du Podcast */
        doc.getRootElement().getChild("channel").getChild("title").setText(nomDuPodcast);

        int maxindex = (doc.getRootElement().getChild("channel").getChildren("item").size() < maxSyncEpisodes) ? doc.getRootElement().getChild("channel").getChildren("item").size() : maxSyncEpisodes;
        DownloadManager dm;
        for (Element item : doc.getRootElement().getChild("channel").getChildren("item").subList(0, maxindex)) {
            URL enclosureUrl = new URL(item.getChild("enclosure").getAttribute("url").getValue());
            try {

                dm = new DownloadManager(enclosureUrl, new File(CurrentDirectory.getAbsolutePath() + "/testfile.out"));
                //dm.download();
                HttpURLConnection connection = (HttpURLConnection) enclosureUrl.openConnection();
                connection.getInputStream().close();
                logger.info(connection.getURL().toString());
                item.getChild("enclosure").setAttribute("url", connection.getURL().toString());
            } catch (IOException ioexception) {
                // ioexception.printStackTrace();
                logger.error(ioexception.getMessage());
            }
			/* Retrait des feedburner tags */
            item.removeChild("origEnclosureLink", feedburnerNS);

        }

		/* Sauvegarde de la version pour Juice : */
        xout.output(doc, new FileOutputStream(CurrentDirectory.getAbsolutePath() + "/" + nomDuPodcast + "ToJuice.xml"));
/*
        for (Element item : doc.getRootElement().getChild("channel").getChildren("item")) {
            String fileNameToLocate = new URL(item.getChild("enclosure").getAttribute("url").getValue()).getFile();
            fileNameToLocate = fileNameToLocate.substring(fileNameToLocate.lastIndexOf("/") + 1);
            logger.debug(fileNameToLocate);
            if (new File(CurrentDirectory.getAbsolutePath() + File.separator + fileNameToLocate).exists()) {
                item.getChild("enclosure").getAttribute("url").setValue((urlDuServeurdePodcast + nomDuPodcast + "/" + fileNameToLocate).replace(" ", "%20"));
            }
        }
*/
        for (Element item : doc.getRootElement().getChild("channel").getChildren("item")) {
            // Récupération du nom de fichier :
            String fileNameToLocate = new URL(item.getChild("enclosure").getAttribute("url").getValue()).getFile();
            fileNameToLocate = fileNameToLocate.substring(fileNameToLocate.lastIndexOf("/") + 1);
            logger.debug(fileNameToLocate);

            Element originalLink = new Element("originalLink");
            originalLink.addContent(new Text(item.getChild("enclosure").getAttribute("url").getValue()));
            item.addContent(originalLink);

            //With Parameters in GET:
            //item.getChild("enclosure").getAttribute("url").setValue((urlDuServeurdePodcast + "download?" + "podcastname=" + nomDuPodcast + "&filename=" + fileNameToLocate).replace(" ", "%20"));

            //With parameters in URL
            item.getChild("enclosure").getAttribute("url").setValue((urlDuServeurdePodcast + "download/" + nomDuPodcast + "/" + fileNameToLocate).replace(" ", "%20"));
            logger.info((urlDuServeurdePodcast + "download/" + nomDuPodcast + "/" + fileNameToLocate).replace(" ", "%20"));
        }


		/* Sauvegarde de la version dk.lan */
        xout.output(doc, new FileOutputStream(CurrentDirectory.getAbsolutePath() + "/" + nomDuPodcast + ".xml"));
    }



}
