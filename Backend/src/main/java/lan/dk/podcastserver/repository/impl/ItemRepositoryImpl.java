package lan.dk.podcastserver.repository.impl;

import javaslang.collection.List;
import javaslang.control.Option;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.custom.ItemRepositoryCustom;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.transform.ResultTransformer;

import javax.transaction.Transactional;
import java.util.Arrays;
import java.util.UUID;

@Slf4j
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private static final String[] SEARCH_FIELDS = new String[]{"description", "title"};
    private static final HibernateIdExtractor RESULT_TRANSFORMER = new HibernateIdExtractor();

    private final FullTextEntityManager fullTextEntityManager;

    public ItemRepositoryImpl(FullTextEntityManager fem) {
        this.fullTextEntityManager = fem;
    }

    @Override
    public void reindex() throws InterruptedException {
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
    public List<UUID> fullTextSearch(String term) {
        if (StringUtils.isEmpty(term))
            return List.empty();

        QueryBuilder qbDsl = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder().forEntity(Item.class).get();

        final BooleanJunction<BooleanJunction> query = qbDsl.bool();
        Arrays.stream(term.split("\\s+"))
                .map(subTerm -> qbDsl.keyword().onFields(SEARCH_FIELDS).matching(subTerm).createQuery())
                .forEach(query::must);

        return Option.of(fullTextEntityManager.createFullTextQuery(query.createQuery(), Item.class)
                .setProjection("id")
                .setResultTransformer(RESULT_TRANSFORMER)
                .<UUID>getResultList())
                .map(l -> (java.util.List<UUID>) l)
                .map(List::ofAll)
                .getOrElse(List.empty());
    }

    static class HibernateIdExtractor implements ResultTransformer {
        @Override
        public Object transformTuple(Object[] tuple, String[] aliases) {
            return tuple[0];
        }

        @Override
        public java.util.List transformList(java.util.List collection) {
            return collection;
        }
    }
}
