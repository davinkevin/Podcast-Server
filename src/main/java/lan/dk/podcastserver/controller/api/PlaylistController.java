package lan.dk.podcastserver.controller.api;

import com.fasterxml.jackson.annotation.JsonView;
import lan.dk.podcastserver.business.PlaylistBusiness;
import lan.dk.podcastserver.entity.Playlist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

import static lan.dk.podcastserver.entity.Playlist.*;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@RestController
@RequestMapping("/api/playlists")
public class PlaylistController {

    @Autowired PlaylistBusiness playlistBusiness;

    @JsonView(PlaylistDetailsListView.class)
    @RequestMapping(method = RequestMethod.POST)
    public Playlist create(@RequestBody Playlist entity) {
        return playlistBusiness.save(entity);
    }

    @JsonView(Object.class)
    @RequestMapping(method = RequestMethod.GET)
    public List<Playlist> findAll() {
        return playlistBusiness.findAll();
    }

    @JsonView(PlaylistDetailsListView.class)
    @RequestMapping(value = "{id}", method = RequestMethod.GET)
    public Playlist findOne(@PathVariable UUID id) {
        return playlistBusiness.findOne(id);
    }

    /*@JsonView(PlaylistDetailsListView.class)
    @RequestMapping(value = "{id}", method = RequestMethod.PUT)
    public Playlist update(@RequestBody Playlist playlist, @PathVariable UUID id) {
        return playlistBusiness.save(playlist.setId(id));
    }*/

    @RequestMapping(method = RequestMethod.DELETE)
    public void delete(UUID uuid) {
        playlistBusiness.delete(uuid);
    }

    @JsonView(PlaylistDetailsListView.class)
    @RequestMapping(value = "{id}/add/{itemId}")
    public Playlist add(@PathVariable UUID id, @PathVariable Integer itemId) {
        return playlistBusiness.add(id, itemId);
    }

    @JsonView(PlaylistDetailsListView.class)
    @RequestMapping(value = "{id}/remove/{itemId}")
    public Playlist remove(@PathVariable UUID id, @PathVariable Integer itemId) {
        return playlistBusiness.remove(id, itemId);
    }
}
