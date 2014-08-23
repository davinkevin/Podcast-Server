package lan.dk.podcastserver.repository.Impl;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.entity.Podcast;
import lan.dk.podcastserver.repository.Custom.ItemRepositoryCustom;
import lan.dk.podcastserver.utils.hibernate.transformer.HibernateIdExtractor;
import org.apache.lucene.search.Query;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.Search;
import org.hibernate.search.query.dsl.QueryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.Date;
import java.util.List;

public class ItemRepositoryImpl implements ItemRepositoryCustom {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @PersistenceContext
    EntityManager em;

    @Override
    public List<Item> findAllItemNotDownloadedNewerThan(Date date) {

        // Récupération des élément inhérant à la recherche par Critères
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Item> criteriaQuery = builder.createQuery(Item.class);

        Root<Item> itemRoot = criteriaQuery.from(Item.class);

        Predicate wherePridcate =
                   builder.and(
                           // Ajout du prédicat de comparaison entre la date du jour et la dâte passé en argument
                           builder.between(itemRoot.<Date>get("pubdate"), date, new Date()),
                           //OR
                           builder.or(
                                   // Status est à Not Downloaded
                                   builder.equal(itemRoot.<String>get("status"), "Not Downloaded"),
                                   // Status est à Null
                                   builder.isNull(itemRoot.<String>get("status"))
                           )
                   );

        // Between two date :
        criteriaQuery.where(wherePridcate);

        // Affichage de la requête pour le Debug
        logger.debug(em.createQuery(criteriaQuery).unwrap(org.hibernate.Query.class).getQueryString());
        return em.createQuery(criteriaQuery).getResultList();

    }

    @Override
    public List<Item> findAllItemDownloadedOlderThan(Date date) {
        // Récupération des élément inhérant à la recherche par Critères
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Item> criteriaQuery = builder.createQuery(Item.class);

        Root<Item> itemRoot = criteriaQuery.from(Item.class);

        Predicate wherePridcate =
                builder.and(
                        builder.lessThan(itemRoot.<Date>get("downloaddate"), date),
                        builder.equal(itemRoot.<String>get("status"), "Finish")
                );

        Join<Item, Podcast> itemPodcastJoin = itemRoot.join("podcast");
        wherePridcate = builder.and(wherePridcate, builder.equal(itemPodcastJoin.<Podcast>get("hasToBeDeleted"), Boolean.TRUE));

        // Between two date :
        criteriaQuery.where(wherePridcate);

        // Affichage de la requête pour le Debug
        logger.debug(em.createQuery(criteriaQuery).unwrap(org.hibernate.Query.class).getQueryString());
        return em.createQuery(criteriaQuery).getResultList();
    }

    @Override
    public void reindex() throws InterruptedException {
        FullTextEntityManager fullTextEntityManager = Search.getFullTextEntityManager(em);
        fullTextEntityManager
                            .createIndexer(Item.class)
                            .startAndWait();
    }

    @Override
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
                .setProjection("id")
                .setResultTransformer(new HibernateIdExtractor())
                .getResultList();

        logger.info(results.toString());

        return results;
    }
}
