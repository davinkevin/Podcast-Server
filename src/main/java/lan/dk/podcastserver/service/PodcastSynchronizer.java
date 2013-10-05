package lan.dk.podcastserver.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PodcastSynchronizer {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
/*
    @Autowired
    private PodcastDAO podcastDAO;

    //@Scheduled(fixedDelay = 600000)
    private void updatePodcast() {
        List<Podcast> podcasts = podcastDAO.getAll();
        Document podcastXML = null;

        for (Podcast podcast : podcasts) {
            try {
                logger.debug("Traitement du Podcast : " + podcast.toString());
                podcastXML = jdom2Parse(podcast.getUrl());

                for (Element item : podcastXML.getRootElement().getChild("channel").getChildren("item")) {

                    Item podcastItem = new Item(item.getChildText("title"), item.getChild("enclosure").getAttributeValue("url"), rfc2822DateToTimeStamp(item.getChildText("pubDate")));
                    logger.debug("Traitement du noeud : " + podcastItem.toString());
                    podcast.getItems().add(podcastItem);
                    podcastItem.setPodcast(podcast);

                }

                podcastDAO.update(podcast);
            } catch (JDOMException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (IOException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }



    }

    private Timestamp rfc2822DateToTimeStamp(String pubDate) throws ParseException {
        Date javaDate = null;
        try {
            String pattern = "EEE, dd MMM yyyy HH:mm:ss Z";
            SimpleDateFormat format = new SimpleDateFormat(pattern, Locale.ENGLISH);
            javaDate = format.parse(pubDate);
            Timestamp timeStampDate = new Timestamp(javaDate.getTime());
            return timeStampDate;
        } catch (ParseException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }


    }

    private Document jdom2Parse(String urlasString) throws JDOMException, IOException {
        SAXBuilder sax = new SAXBuilder();
        URL url;
        Document doc = null;
        try {
            url = new URL(urlasString);
            doc = sax.build(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        } catch (JDOMException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        } catch (IOException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            throw e;
        }

        return doc;

    }*/
}
