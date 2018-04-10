package lan.dk.podcastserver.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
public class HomeController {

    @RequestMapping(value = {"", "items", "podcasts", "podcasts/**", "player", "download", "stats"})
    public String v1() {
        return "index";
    }

    @RequestMapping(value = {"v2", "v2/search", "v2/items", "v2/podcasts", "v2/podcasts/**", "v2/player", "v2/download", "v2/stats"})
    public String v2() {
        return "v2/index";
    }
}
