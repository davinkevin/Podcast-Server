package lan.dk.podcastserver.repository.impl;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.custom.ItemRepositoryCustom;
import lan.dk.podcastserver.utils.hibernate.transformer.HibernateIdExtractor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hibernate.search.jpa.Search.getFullTextEntityManager;

public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private static final HibernateIdExtractor RESULT_TRANSFORMER = new HibernateIdExtractor();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    public static final String[] SEARCH_FIELDS = new String[]{"description", "title"};

    @PersistenceContext EntityManager em;

    @Override
    public void reindex() throws InterruptedException {
        FullTextEntityManager fullTextEntityManager = getFullTextEntityManager(em);
        fullTextEntityManager
                .createIndexer(Item.class)
                .batchSizeToLoadObjects(25)
                .cacheMode(CacheMode.NORMAL)
                .idFetchSize(150)
                .threadsToLoadObjects(1)
                .progressMonitor(new SimpleIndexingProgressMonitor(1000))
                .startAndWait();
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public List<Integer> fullTextSearch(String term) {
        if (StringUtils.isEmpty(term))
            return new ArrayList<>();

        FullTextEntityManager fullTextEntityManager = getFullTextEntityManager(em);

        QueryBuilder qbDsl = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder().forEntity(Item.class).get();

        final BooleanJunction<BooleanJunction> query = qbDsl.bool();
        Arrays.stream(term.split("\\s+"))
                .map(subTerm -> qbDsl.keyword().onFields(SEARCH_FIELDS).matching(subTerm).createQuery())
                .forEach(query::must);

        List<Integer> results = fullTextEntityManager.createFullTextQuery(query.createQuery(), Item.class)
                .setProjection("id")
                .setResultTransformer(RESULT_TRANSFORMER)
                .<Integer>getResultList();

        if (results == null) {
            return new ArrayList<>();
        }

        logger.debug(results.toString());
        return results;
    }
}
