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
}
