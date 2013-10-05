package lan.dk.podcastserver.repository.Impl;

import lan.dk.podcastserver.entity.Item;
import lan.dk.podcastserver.repository.Custom.ItemRepositoryCustom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
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
                        // Ajout du prédicat de comparaison entre la date du jour et la dâte passé en argument
                        //builder.between(itemRoot.<Date>get("pubdate"), date, new Date()),
                        //builder.greaterThan(itemRoot.<Date>get("downloaddate"), date),
                        builder.lessThan(itemRoot.<Date>get("downloaddate"), date),
                        builder.equal(itemRoot.<String>get("status"), "Finish")
                        //OR
                        /* builder.or(
                                // Status est à Not Downloaded
                                builder.equal(itemRoot.<String>get("status"), "Not Downloaded"),
                                // Status est à Null
                                builder.isNull(itemRoot.<String>get("status"))
                        ) */
                );

        // Between two date :
        criteriaQuery.where(wherePridcate);

        // Affichage de la requête pour le Debug
        logger.debug(em.createQuery(criteriaQuery).unwrap(org.hibernate.Query.class).getQueryString());
        return em.createQuery(criteriaQuery).getResultList();
    }
}
