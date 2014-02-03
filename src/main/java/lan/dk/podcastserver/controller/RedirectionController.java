package lan.dk.podcastserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;


@Controller
@RequestMapping("/")
public class RedirectionController {

    @RequestMapping(value = {"", "home"}, method = RequestMethod.GET)
    private String homePodcast() {
        return "index";
    }
    /*
    @RequestMapping(value = {"list"}, method = RequestMethod.GET)
    private String listPodcast() {
        return "podcasts";
    }

    @RequestMapping(value = "add", method = RequestMethod.GET)
    private String addPodcast() {
        return "addPodcast";
    }

    @RequestMapping(value = "downloadList", method = RequestMethod.GET)
    private String downloadList() {
        return "downloadList";
    }

    @RequestMapping(value = "angular", method = RequestMethod.GET)
    private String angular() {
        return "podcastserver";
    }
    */

}
