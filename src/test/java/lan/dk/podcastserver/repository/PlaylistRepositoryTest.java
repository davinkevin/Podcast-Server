package lan.dk.podcastserver.repository;

import com.ninja_squad.dbsetup.DbSetup;
import com.ninja_squad.dbsetup.DbSetupTracker;
import com.ninja_squad.dbsetup.destination.DataSourceDestination;
import com.ninja_squad.dbsetup.operation.Operation;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Playlist;
import lan.dk.podcastserver.entity.PlaylistAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.ConfigFileApplicationContextInitializer;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.util.Set;
import java.util.UUID;

import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.operation.CompositeOperation.sequenceOf;
import static lan.dk.podcastserver.repository.DatabaseConfiguraitonTest.DELETE_ALL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by kevin on 17/01/2016 for PodcastServer
 */
@Transactional
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {DatabaseConfiguraitonTest.class, HibernateJpaAutoConfiguration.class}, initializers = ConfigFileApplicationContextInitializer.class)
public class PlaylistRepositoryTest {

    @Autowired DataSource dataSource;
    @Autowired PlaylistRepository playlistRepository;
    @Autowired ItemRepository itemRepository;

    private final static DbSetupTracker dbSetupTracker = new DbSetupTracker();
    public static final Operation INSERT_PLAYLIST_DATA = sequenceOf(
            ItemRepositoryTest.INSERT_ITEM_DATA,
            insertInto("PLAYLIST")
                    .columns("ID", "NAME")
                    .values(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), "Humour Playlist")
                    .values(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"), "Conférence Rewind")
                    .build(),
            insertInto("PLAYLIST_ITEMS")
                    .columns("PLAYLIST_ID", "ITEMS_ID")
                    .values(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), 3)
                    .values(UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66"), 5)
                    .values(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"), 5)
                    .build()
    );

    @Before
    public void prepare() throws Exception {
        Operation operation = sequenceOf(DELETE_ALL, INSERT_PLAYLIST_DATA);
        DbSetup dbSetup = new DbSetup(new DataSourceDestination(dataSource), operation);

        dbSetupTracker.launchIfNecessary(dbSetup);
    }

    @Test
    public void should_save_a_playlist() {
        /* Given */
        Playlist playlist = Playlist.builder().name("A New Playlist").build();

        /* When */
        Playlist savedPlaylist = playlistRepository.save(playlist);

        /* Then */
        assertThat(savedPlaylist.getId()).isNotNull();
    }

    @Test
    public void should_find_by_id() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        UUID id = UUID.fromString("dc024a30-bd02-11e5-a837-0800200c9a66");

        /* When */
        Playlist playlist = playlistRepository.findOne(id);

        /* Then */
        assertThat(playlist).isNotNull();
        assertThat(playlist.getItems()).hasSize(2);
        PlaylistAssert
                .assertThat(playlist)
                .hasId(id)
                .hasName("Humour Playlist");
    }

    @Test
    public void should_find_playlist_having_item() {
        /* Given */
        dbSetupTracker.skipNextLaunch();
        Item item = new Item().setId(5);

        /* When */
        Set<Playlist> playlists = playlistRepository.findContainsItem(item);

        /* Then */
        assertThat(playlists).hasSize(2);
    }

    @Test
    public void should_add_a_item_to_playlist() {
        /* Given */
        Item item = itemRepository.findOne(4);
        Playlist playlist = playlistRepository.findOne(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"));

        /* When */
        playlistRepository.save(playlist.add(item));
        playlistRepository.flush();
        Playlist fetchedPlaylist = playlistRepository.findOne(UUID.fromString("24248480-bd04-11e5-a837-0800200c9a66"));

        /* Then */
        assertThat(fetchedPlaylist.getItems()).hasSize(2);
    }




}