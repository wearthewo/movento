package com.movento.contentservice.catalog;

import com.movento.contentservice.dto.CatalogTitleResponse;
import com.movento.contentservice.model.Movie;
import com.movento.contentservice.repository.ContentRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogServiceTest {

    private ContentRepository repository;
    private CatalogService catalog;

    @BeforeEach
    void setUp() {
        repository = mock(ContentRepository.class);
        catalog = new CatalogService(repository, mock(JdbcTemplate.class));
    }

    @AfterEach
    void tearDown() {
        catalog.stopRefresher();
    }

    @Test
    void homeIsBuiltOnceAndThenServedFromTheSnapshot() {
        Movie hero = movie(1L, "hero", "PG-13", true);
        Movie other = movie(2L, "other", "PG", false);
        when(repository.findActiveIdsNewestFirst(any())).thenReturn(List.of(1L, 2L));
        // The fetch join may repeat a title once per genre row; that must not break the snapshot.
        when(repository.findByIds(anyList())).thenReturn(List.of(hero, other, hero));

        CatalogService.HomeFeed first = catalog.home(MaturityPolicy.ADULT);
        CatalogService.HomeFeed second = catalog.home(MaturityPolicy.ADULT);

        assertThat(first.featured().slug()).isEqualTo("hero");
        assertThat(second).isSameAs(first);
        verify(repository, times(1)).findByIds(anyList());
    }

    @Test
    void kidsSnapshotLeavesOutAdultTitles() {
        when(repository.findActiveIdsNewestFirst(any())).thenReturn(List.of(1L, 2L));
        when(repository.findByIds(anyList())).thenReturn(List.of(movie(1L, "family", "PG", true), movie(2L, "thriller", "R", false)));

        List<String> slugs = catalog.home(MaturityPolicy.KIDS).rails().stream()
                .flatMap(rail -> rail.items().stream())
                .map(CatalogTitleResponse::slug)
                .distinct()
                .toList();

        assertThat(slugs).containsExactly("family");
    }

    @Test
    void aCatalogChangeIsVisibleOnTheNextRequest() {
        when(repository.findActiveIdsNewestFirst(any())).thenReturn(List.of(1L));
        when(repository.findByIds(anyList())).thenReturn(List.of(movie(1L, "old", "PG", true)));
        assertThat(catalog.home(MaturityPolicy.ADULT).featured().slug()).isEqualTo("old");

        when(repository.findActiveIdsNewestFirst(any())).thenReturn(List.of(2L));
        when(repository.findByIds(anyList())).thenReturn(List.of(movie(2L, "new", "PG", true)));
        catalog.invalidate();

        assertThat(catalog.home(MaturityPolicy.ADULT).featured().slug()).isEqualTo("new");
    }

    private static Movie movie(Long id, String slug, String rating, boolean featured) {
        Movie movie = new Movie();
        movie.setId(id);
        movie.setSlug(slug);
        movie.setTitle(slug);
        movie.setContentRating(rating);
        movie.setFeatured(featured);
        movie.setTrending(true);
        movie.setReleaseYear(2024);
        return movie;
    }
}
