package lan.dk.podcastserver.repository.impl;

import com.google.common.collect.Lists;
import lan.dk.podcastserver.entity.Item;
import org.apache.lucene.search.Query;
import org.hibernate.CacheMode;
import org.hibernate.search.MassIndexer;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.batchindexing.impl.SimpleIndexingProgressMonitor;
import org.hibernate.search.jpa.FullTextEntityManager;
import org.hibernate.search.jpa.FullTextQuery;
import org.hibernate.search.query.dsl.*;
import org.hibernate.transform.ResultTransformer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by kevin on 05/09/15 for Podcast Server
 */
@RunWith(MockitoJUnitRunner.class)
public class ItemRepositoryImplTest {

    @Mock FullTextEntityManager fullTextEntityManager;
    @InjectMocks ItemRepositoryImpl itemRepositoryImpl;

    @Test
    public void should_reindex() throws InterruptedException {
        /* Given */
        MassIndexer massIndexer = mock(MassIndexer.class);
        when(fullTextEntityManager.createIndexer(any())).thenReturn(massIndexer);
        when(massIndexer.batchSizeToLoadObjects(anyInt())).thenReturn(massIndexer);
        when(massIndexer.cacheMode(any())).thenReturn(massIndexer);
        when(massIndexer.idFetchSize(anyInt())).thenReturn(massIndexer);
        when(massIndexer.threadsToLoadObjects(anyInt())).thenReturn(massIndexer);
        when(massIndexer.progressMonitor(any())).thenReturn(massIndexer);

        /* When */
        itemRepositoryImpl.reindex();

        /* Then */
        verify(fullTextEntityManager, times(1)).createIndexer(eq(Item.class));
        verify(massIndexer, times(1)).batchSizeToLoadObjects(eq(25));
        verify(massIndexer, times(1)).cacheMode(eq(CacheMode.NORMAL));
        verify(massIndexer, times(1)).idFetchSize(eq(150));
        verify(massIndexer, times(1)).threadsToLoadObjects(eq(1));
        verify(massIndexer, times(1)).progressMonitor(isA(SimpleIndexingProgressMonitor.class));
        verify(massIndexer, times(1)).startAndWait();
    }

    @Test
    public void should_return_empty_array_if_no_word() {
        assertThat(itemRepositoryImpl.fullTextSearch("")).hasSize(0);
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void should_search_fulltext() {
        /* Given */
        List<UUID> results = Lists.newArrayList(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        SearchFactory searchFactory = mock(SearchFactory.class);
        QueryContextBuilder queryContextBuilder = mock(QueryContextBuilder.class);
        EntityContext entityContext = mock(EntityContext.class);
        QueryBuilder queryBuilder = mock(QueryBuilder.class);
        BooleanJunction booleanJunction = mock(BooleanJunction.class);
        TermContext termContext = mock(TermContext.class);
        TermMatchingContext termMatchingContext = mock(TermMatchingContext.class);
        TermTermination termTermination = mock(TermTermination.class);
        FullTextQuery fullTextQuery = mock(FullTextQuery.class);

        when(fullTextEntityManager.getSearchFactory()).thenReturn(searchFactory);
        when(searchFactory.buildQueryBuilder()).thenReturn(queryContextBuilder);
        when(queryContextBuilder.forEntity(any())).thenReturn(entityContext);
        when(entityContext.get()).thenReturn(queryBuilder);
        when(queryBuilder.bool()).thenReturn(booleanJunction);
        when(queryBuilder.keyword()).thenReturn(termContext);
        when(termContext.onFields(anyVararg())).thenReturn(termMatchingContext);
        when(termMatchingContext.matching(anyString())).thenReturn(termTermination);
        when(termTermination.createQuery()).thenReturn(mock(Query.class));
        when(booleanJunction.must(any())).thenReturn(mock(MustJunction.class));
        when(fullTextEntityManager.createFullTextQuery(any(), any())).thenReturn(fullTextQuery);
        when(booleanJunction.createQuery()).thenReturn(mock(Query.class));
        when(fullTextQuery.setProjection(anyString())).thenReturn(fullTextQuery);
        when(fullTextQuery.setResultTransformer(any(ResultTransformer.class))).thenReturn(fullTextQuery);
        when(fullTextQuery.getResultList()).thenReturn(results);

        /* When */
        List<UUID> list = itemRepositoryImpl.fullTextSearch("A super query");

        /* Then */
        assertThat(list)
                .containsExactlyElementsOf(results);

        verify(termMatchingContext, times(3)).matching(or(or(eq("A"), eq("super")), eq("query")));
        verify(booleanJunction, times(3)).must(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void should_search_fulltext_and_find_nothing() {
        /* Given */
        SearchFactory searchFactory = mock(SearchFactory.class);
        QueryContextBuilder queryContextBuilder = mock(QueryContextBuilder.class);
        EntityContext entityContext = mock(EntityContext.class);
        QueryBuilder queryBuilder = mock(QueryBuilder.class);
        BooleanJunction booleanJunction = mock(BooleanJunction.class);
        TermContext termContext = mock(TermContext.class);
        TermMatchingContext termMatchingContext = mock(TermMatchingContext.class);
        TermTermination termTermination = mock(TermTermination.class);
        FullTextQuery fullTextQuery = mock(FullTextQuery.class);

        when(fullTextEntityManager.getSearchFactory()).thenReturn(searchFactory);
        when(searchFactory.buildQueryBuilder()).thenReturn(queryContextBuilder);
        when(queryContextBuilder.forEntity(any())).thenReturn(entityContext);
        when(entityContext.get()).thenReturn(queryBuilder);
        when(queryBuilder.bool()).thenReturn(booleanJunction);
        when(queryBuilder.keyword()).thenReturn(termContext);
        when(termContext.onFields(anyVararg())).thenReturn(termMatchingContext);
        when(termMatchingContext.matching(anyString())).thenReturn(termTermination);
        when(termTermination.createQuery()).thenReturn(mock(Query.class));
        when(booleanJunction.must(any())).thenReturn(mock(MustJunction.class));
        when(fullTextEntityManager.createFullTextQuery(any(), any())).thenReturn(fullTextQuery);
        when(booleanJunction.createQuery()).thenReturn(mock(Query.class));
        when(fullTextQuery.setProjection(anyString())).thenReturn(fullTextQuery);
        when(fullTextQuery.setResultTransformer(any(ResultTransformer.class))).thenReturn(fullTextQuery);
        when(fullTextQuery.getResultList()).thenReturn(null);

        /* When */
        List<UUID> list = itemRepositoryImpl.fullTextSearch("A super query");

        /* Then */
        assertThat(list).hasSize(0);
    }

}