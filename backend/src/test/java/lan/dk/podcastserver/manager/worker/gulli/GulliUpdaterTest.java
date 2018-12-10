package lan.dk.podcastserver.manager.worker.gulli;

import com.github.davinkevin.podcastserver.service.HtmlService;
import com.github.davinkevin.podcastserver.service.ImageService;
import com.github.davinkevin.podcastserver.service.SignatureService;
import io.vavr.collection.Set;
import lan.dk.podcastserver.entity.Cover;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.service.properties.PodcastServerParameters;
import com.github.davinkevin.podcastserver.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.validation.Validator;
import java.io.IOException;
import java.net.URISyntaxException;

import static io.vavr.API.None;
import static lan.dk.podcastserver.assertion.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Created by kevin on 14/10/2016
 */
@RunWith(MockitoJUnitRunner.class)
public class GulliUpdaterTest {

    private @Mock PodcastServerParameters podcastServerParameters;
    private @Mock SignatureService signatureService;
    private @Mock Validator validator;
    private @Mock HtmlService htmlService;
    private @Mock ImageService imageService;
    private @InjectMocks
    GulliUpdater gulliUpdater;

    Podcast podcast;

    @Before
    public void beforeEach() {
        podcast = Podcast
                    .builder()
                        .url("http://replay.gulli.fr/dessins-animes/Pokemon3")
                        .title("Pokemon")
                    .build();
    }

    @Test
    public void should_get_signature() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(podcast.getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/gulli/pokemon.html"));
        when(signatureService.fromText(anyString())).thenCallRealMethod();

        /* When */
        String signature = gulliUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("4d0bb11a29d851eabf10245b00d4cabe");
    }

    @Test
    public void should_return_empty_string_if_error_during_signature() {
        /* Given */
        when(htmlService.get(anyString())).thenReturn(None());

        /* When */
        String signature = gulliUpdater.signatureOf(podcast);

        /* Then */
        assertThat(signature).isEqualTo("");
    }

    @Test
    public void should_return_list_of_items() throws IOException, URISyntaxException {
        /* Given */
        when(htmlService.get(podcast.getUrl())).thenReturn(IOUtils.fileAsHtml("/remote/podcast/gulli/pokemon.html"));
        when(htmlService.get("http://replay.gulli.fr/dessins-animes/Pokemon3/VOD68526621555000")).thenReturn(IOUtils.fileAsHtml("/remote/podcast/gulli/VOD68526621555000.html"));
        when(htmlService.get("http://replay.gulli.fr/dessins-animes/Pokemon3/VOD68526621609000")).thenReturn(IOUtils.fileAsHtml("/remote/podcast/gulli/VOD68526621609000.html"));
        when(imageService.getCoverFromURL(anyString())).then(i -> Cover.builder().url(i.getArgument(0)).height(200).width(200).build());

        /* When */
        Set<Item> items = gulliUpdater.getItems(podcast);
        Item first = items.filter(i -> i.getTitle().contains("13")).getOrElseThrow(() -> new RuntimeException("Episode 13 Not Found"));
        Item second = items.filter(i -> i.getTitle().contains("14")).getOrElseThrow(() -> new RuntimeException("Episode 14 Not Found"));;

        /* Then */
        assertThat(items)
                .isNotEmpty()
                .hasSize(2);

        assertThat(first)
            .hasTitle("Saison 19, Episode 13 : Voyages croisés")
            .hasUrl("http://replay.gulli.fr/jwplayer/embed/VOD68526621555000")
            .hasDescription("Sacha et Liam livrent un combat, et Amphinobi et Jungko, maintenant évolués, sont passionnés par cette revanche ! Non loin de là, Alain, qui est en voyage pour étudier la Méga-Évolution, s'arrête pour regarder le combat, et il est très intrigué de voir qu'Amphinobi semble se transformer, juste avant de vaincre Jungko. Alain veut en savoir davantage sur les possibilités de l'Amphinobi de Sacha, et il le met au défi de combattre son Dracaufeu. Lorsque Dracaufeu méga-évolue et qu'Amphinobi change à nouveau d'apparence, le combat devient intense ! Amphinobi finit par être vaincu mais tous souhaitent en savoir plus sur cette mystérieuse transformation ! Pendant ce temps, les expériences de la Team Flare semblent progresser...");
        assertThat(first.getCover())
                .hasUrl("http://resize1-gulli.ladmedia.fr/r/280,210,smartcrop,center-top/img/var/storage/imports/replay/images/custom/thumbnails/snapshot_VOD68526621555000_20161007-141504.png");

        assertThat(second)
                .hasTitle("Saison 19, Episode 14 : Une opération explosive")
                .hasUrl("http://replay.gulli.fr/jwplayer/embed/VOD68526621609000")
                .hasDescription("À la recherche de la Team Flare et de Pouic, la Team Rocket rencontre un Pokémon qui lui ressemble, celui que l'on connait sous le nom de Z2...et la Team Flare est sur ses traces ! Les deux équipes s'affrontent, et Z2 change plusieurs fois de ravisseurs jusqu'au moment où il fusionne avec de nombreuses Cellules pour devenir un Pokémon reptilien puissant et menaçant, dont les attaques bousculent ses ravisseurs ! Z2 prend l'avantage, mais Lysandre a appelé du renfort ! Avec l'aide d'Alain et de son Méga-Dracaufeu, la Team Flare affronte Z2 et l'enferme dans une cage. Pendant ce temps, au campement de nos héros, Clem câline un Pouic triste et inquiet, en essayant de comprendre ce qui ne va pas...");
        assertThat(second.getCover())
                .hasUrl("http://resize1-gulli.ladmedia.fr/r/280,210,smartcrop,center-top/img/var/storage/imports/replay/images/custom/thumbnails/snapshot_VOD68526621609000_20161007-142635.png");

    }

    @Test
    public void should_be_of_type_gulli() {
        assertThat(gulliUpdater.type().key()).isEqualTo("Gulli");
        assertThat(gulliUpdater.type().name()).isEqualTo("Gulli");
    }

    @Test
    public void should_be_compatible() {
        assertThat(gulliUpdater.compatibility("http://replay.gulli.fr/dessins-animes/Pokemon3"))
                .isEqualTo(1);
    }

    @Test
    public void should_not_be_compatible() {
        assertThat(gulliUpdater.compatibility("http://foo.bar.fr/dessins-animes/Pokemon3"))
                .isGreaterThan(1);
    }
}
