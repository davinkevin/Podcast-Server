package lan.dk.podcastserver.repository;

import lan.dk.podcastserver.entity.Podcast;
import org.junit.*;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.Date;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes =lan.dk.podcastserver.context.JPAContextConfiguration.class, loader=AnnotationConfigContextLoader.class)
public class PodcastRepositoryTest {

        private static final Logger logger = LoggerFactory.getLogger(PodcastRepositoryTest.class);

        @Autowired
        PodcastRepository podcastRepository;

        @BeforeClass
        public static void oneTimeSetUp() {

        }

        @AfterClass
        public static void oneTimeTearDown() {

        }

        @Before
        public void setUp() {

        }

        @After
        public void tearDown() {

        }

        @Test
        public void testCreate() {
            /*Podcast p = new Podcast("Le Rendez-vous Tech", "http://feeds2.feedburner.com/lerendezvoustech", null, "rss", null, "http://lrdv.fr/audio/lrdvtech140.jpg", 200, 200);
            p = podcastRepository.save(p);
            logger.debug("bla");
            assertTrue(p.getId() != 0);*/

        }



    @Test
        public void testUpdate() {
            Podcast p = podcastRepository.findOne(20);
            Date d = new Date();

            p.setType("Type" + p.getId() + " " + d.toString());

            podcastRepository.save(p);
        }
    }
