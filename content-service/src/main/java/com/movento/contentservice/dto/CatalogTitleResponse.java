package com.movento.contentservice.dto;

import java.util.List;
public record CatalogTitleResponse(String id, String slug, String title, String synopsis, String type,
 Integer releaseYear, String maturityRating, Integer runtimeMinutes, List<String> genres,
 String artworkUrl, String backdropUrl, String trailerUrl, boolean featured, boolean trending) {}
