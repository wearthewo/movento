package com.movento.contentservice.controller;

import com.movento.contentservice.dto.CatalogTitleResponse;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.Movie;
import com.movento.contentservice.repository.ContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController @RequestMapping("/api/v1/catalog") @RequiredArgsConstructor
public class CatalogController {
    private final ContentRepository content;

    @GetMapping("/home")
    public Map<String, Object> home() {
        List<Content> recent = content.findTop12ByActiveTrueOrderByCreatedAtDesc();
        Content hero = content.findFirstByActiveTrueAndFeaturedTrueOrderByCreatedAtDesc().orElseGet(() -> recent.stream().findFirst().orElseThrow());
        List<Map<String, Object>> rails = List.of(
            rail("trending", "Trending now", content.findTop12ByActiveTrueAndTrendingTrueOrderByCreatedAtDesc()),
            rail("new", "New on Movento", recent)
        );
        Map<String, Object> result = new LinkedHashMap<>(); result.put("featured", map(hero)); result.put("rails", rails); return result;
    }

    @GetMapping("/titles")
    public List<CatalogTitleResponse> titles(@RequestParam(required = false) String type, @RequestParam(required = false) String q) {
        List<Content> items = q == null || q.isBlank()
            ? content.findByActiveTrue(PageRequest.of(0, 100)).stream().toList()
            : content.searchCatalog(q.trim(), PageRequest.of(0, 100));
        Map<Long, Content> deduped = new LinkedHashMap<>();
        items.stream()
            .filter(item -> type == null || type.equals(item instanceof Movie ? "MOVIE" : "SERIES"))
            .forEach(item -> deduped.putIfAbsent(item.getId(), item));
        return deduped.values().stream().map(this::map).toList();
    }

    @GetMapping("/search") public List<CatalogTitleResponse> search(@RequestParam(defaultValue = "") String q) { return titles(null, q); }
    @GetMapping("/titles/{slug}") public CatalogTitleResponse title(@PathVariable String slug) { return map(content.findBySlugAndActiveTrue(slug).orElseThrow()); }

    private Map<String, Object> rail(String id, String title, List<Content> items) { return Map.of("id", id, "title", title, "items", items.stream().map(this::map).toList()); }
    private CatalogTitleResponse map(Content item) { return new CatalogTitleResponse(String.valueOf(item.getId()), item.getSlug(), item.getTitle(), item.getDescription(), item instanceof Movie ? "MOVIE" : "SERIES", item.getReleaseYear(), item.getContentRating(), item.getDurationMinutes(), item.getGenres().stream().map(g -> g.getName()).sorted().toList(), item.getThumbnailUrl(), item.getBackdropUrl(), null, item.isFeatured(), item.isTrending()); }
}
