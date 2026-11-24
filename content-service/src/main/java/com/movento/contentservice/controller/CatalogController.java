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
                                             @RequestParam(required = false) List<Long> ids) {
        MaturityPolicy policy = MaturityPolicy.fromHeader(maturity);
        if (ids != null && !ids.isEmpty()) return catalog.byIds(policy, ids.stream().limit(100).toList());
        return catalog.titles(policy, type, q, genre);
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

    @GetMapping("/titles/{slug}/similar")
    public List<CatalogTitleResponse> similar(@RequestHeader(value = MATURITY, required = false) String maturity, @PathVariable String slug) {
        return catalog.similar(MaturityPolicy.fromHeader(maturity), slug);
    }
}
