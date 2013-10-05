/*
package lan.dk.podcastserver.controller;

import lan.dk.podcastserver.entityold.Podcast;
import lan.dk.podcastserver.entityold.PodcastCovertArt;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.DOMBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/")
public class ListPodcastController {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Value("${rootfolder}")
    protected String rootFolder;

    @Value("${serverURL}")
    protected String serverURL;

    @RequestMapping(method = RequestMethod.GET)
	public String printWelcome(ModelMap model) {
		model.addAttribute("message", rootFolder);

        List<Podcast> listOfPodcast = new ArrayList<Podcast>();

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
            logger.info("Affichage du dossier " + podcast.getChild("title").getValue());
            listOfPodcast.add(new Podcast(podcast.getChild("title").getValue(), podcast.getChild("flux").getValue(), new PodcastCovertArt(Integer.valueOf(podcast.getChild("image").getAttributeValue("width")), Integer.valueOf(podcast.getChild("image").getAttributeValue("height")), podcast.getChild("image").getValue())));

        }

        model.addAttribute("listOfPodcast", listOfPodcast);
        model.addAttribute("serverURL", serverURL);
        //return rootFolder;

		return "listPodcast";
	}

}*/
