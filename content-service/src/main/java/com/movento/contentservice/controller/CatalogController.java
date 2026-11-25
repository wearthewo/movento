package com.movento.contentservice.controller;

import com.movento.contentservice.catalog.CatalogService;
import com.movento.contentservice.catalog.MaturityPolicy;
import com.movento.contentservice.dto.CatalogTitleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Public catalog API. The gateway runs optional authentication on these routes, so when a
 * profile-scoped token is present X-Profile-Maturity is set and results are filtered (kids profiles).
 */
@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
public class CatalogController {
    private static final String MATURITY = "X-Profile-Maturity";

    private final CatalogService catalog;

    @GetMapping("/home")
    public CatalogService.HomeFeed home(@RequestHeader(value = MATURITY, required = false) String maturity) {
        return catalog.home(MaturityPolicy.fromHeader(maturity));
    }

    @GetMapping("/titles")
    public List<CatalogTitleResponse> titles(@RequestHeader(value = MATURITY, required = false) String maturity,
                                             @RequestParam(required = false) String type,
                                             @RequestParam(required = false) String q,
                                             @RequestParam(required = false) String genre,
                                             @RequestParam(required = false) List<Long> ids,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "100") int size,
                                             jakarta.servlet.http.HttpServletResponse response) {
        MaturityPolicy policy = MaturityPolicy.fromHeader(maturity);
        if (ids != null && !ids.isEmpty()) return catalog.byIds(policy, ids.stream().limit(100).toList());
        List<CatalogTitleResponse> all = catalog.titles(policy, type, q, genre);
        int pageSize = Math.max(1, Math.min(size, 100));
        int from = Math.min(Math.max(page, 0) * pageSize, all.size());
        response.setHeader("X-Total-Count", String.valueOf(all.size()));
        return all.subList(from, Math.min(from + pageSize, all.size()));
    }

    @GetMapping("/search")
    public List<CatalogTitleResponse> search(@RequestHeader(value = MATURITY, required = false) String maturity,
                                             @RequestParam(defaultValue = "") String q) {
        return catalog.titles(MaturityPolicy.fromHeader(maturity), null, q, null);
    }

    @GetMapping("/titles/{slug}")
    public CatalogTitleResponse title(@RequestHeader(value = MATURITY, required = false) String maturity, @PathVariable String slug) {
        return catalog.title(MaturityPolicy.fromHeader(maturity), slug);
    }

    @GetMapping("/titles/{slug}/episodes")
    public List<CatalogService.SeasonResponse> episodes(@RequestHeader(value = MATURITY, required = false) String maturity, @PathVariable String slug) {
        return catalog.episodes(MaturityPolicy.fromHeader(maturity), slug);
    }

    /** Seed title + similar titles in one call; {@code exclude} = ids the profile already started. */
    @GetMapping("/titles/by-id/{id}/similar")
    public CatalogService.SimilarTo similarById(@RequestHeader(value = MATURITY, required = false) String maturity,
                                                @PathVariable Long id,
                                                @RequestParam(required = false) List<Long> exclude) {
        java.util.Set<Long> excluded = exclude == null ? java.util.Set.of() : java.util.Set.copyOf(exclude.stream().limit(200).toList());
        return catalog.similarById(MaturityPolicy.fromHeader(maturity), id, excluded);
    }

    @GetMapping("/titles/{slug}/similar")
    public List<CatalogTitleResponse> similar(@RequestHeader(value = MATURITY, required = false) String maturity, @PathVariable String slug) {
        return catalog.similar(MaturityPolicy.fromHeader(maturity), slug);
    }
}
