package lan.dk.podcastserver.repository.impl;

import io.vavr.collection.List;
import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.custom.ItemRepositoryCustom;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.CacheMode;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.BooleanJunction;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.hibernate.transform.ResultTransformer;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.util.Arrays;
import java.util.UUID;

import static io.vavr.API.Option;

@Slf4j
@RequiredArgsConstructor
public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private static final String[] SEARCH_FIELDS = new String[]{"description", "title"};
    private static final HibernateIdExtractor RESULT_TRANSFORMER = new HibernateIdExtractor();

    private final EntityManager entityManager;

    @Override
    @Transactional
    public void reindex() throws InterruptedException {
        Search.getFullTextEntityManager(entityManager)
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
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(entityManager);
        if (StringUtils.isEmpty(term))
            return List.empty();

        QueryBuilder qbDsl = fullTextEntityManager.getSearchFactory()
                .buildQueryBuilder().forEntity(Item.class).get();

        final BooleanJunction<BooleanJunction> query = qbDsl.bool();
        Arrays.stream(term.split("\\s+"))
                .map(subTerm -> qbDsl.keyword()
                        .onField("description").boostedTo(2F)
                        .andField("title").matching(subTerm)
                        .createQuery()
                )
                .forEach(query::must);

        return Option(fullTextEntityManager.createFullTextQuery(query.createQuery(), Item.class)
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
