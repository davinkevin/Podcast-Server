package lan.dk.podcastserver.repository.impl;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.custom.ItemRepositoryCustom;
import lan.dk.podcastserver.utils.hibernate.transformer.HibernateIdExtractor;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.Query;
import org.hibernate.CacheMode;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.search.jpa.Search.getFullTextEntityManager;

public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @PersistenceContext
    EntityManager em;

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
    @SuppressWarnings("unchecked")
    public List<Integer> fullTextSearch(String term) {
        if (StringUtils.isEmpty(term))
            return new ArrayList<>();

        FullTextEntityManager fullTextEntityManager = getFullTextEntityManager(em);

        QueryBuilder qbDsl = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder().forEntity(Item.class).get();

        Query luceneQuery = qbDsl.keyword()
                .onFields("description", "title")
                .matching(term).createQuery();

        List results = fullTextEntityManager.createFullTextQuery(luceneQuery, Item.class)
                //.setSort(new org.apache.lucene.search.Sort(new SortField(null, SortField.SCORE, getLuceneOrder(direction))))
                .setProjection("id")
                .setResultTransformer(new HibernateIdExtractor())
                .getResultList();

        logger.debug(results.toString());
        return results;
    }
}
