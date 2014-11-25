package lan.dk.podcastserver.repository.Impl;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.Custom.ItemRepositoryCustom;
import lan.dk.podcastserver.utils.hibernate.transformer.HibernateIdExtractor;
import org.apache.lucene.search.Query;
import org.hibernate.CacheMode;
import org.hibernate.search.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @PersistenceContext
    EntityManager em;

    @Override
    public void reindex() throws InterruptedException {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
        fullTextEntityManager
                            .createIndexer(Item.class)
                            .batchSizeToLoadObjects(25)
                            .cacheMode(CacheMode.NORMAL)
                            .idFetchSize(150)
                            .threadsToLoadObjects(1)
                            .progressMonitor(new SimpleIndexingProgressMonitor(100))
                            .startAndWait();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Integer> fullTextSearch(String term) {
        FullTextEntityManager fullTextEntityManager =
                org.hibernate.search.jpa.Search.getFullTextEntityManager(em);

        QueryBuilder qbDsl = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder().forEntity( Item.class ).get();

        Query luceneQuery = qbDsl.keyword()
                                .fuzzy()
                                    .withThreshold(0.7f)
                                    .withPrefixLength(3)
                            .onFields("description", "title")
                            .matching(term).createQuery();

        List results = fullTextEntityManager.createFullTextQuery(luceneQuery, Item.class)
                //.setSort(new org.apache.lucene.search.Sort(new SortField(null, SortField.SCORE, getLuceneOrder(direction))))
                .setProjection("id")
                .setResultTransformer(new HibernateIdExtractor())
                .getResultList();

        logger.info(results.toString());
        return results;
    }
}
