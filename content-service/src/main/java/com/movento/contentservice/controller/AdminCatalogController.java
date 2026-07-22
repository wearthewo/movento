package com.movento.contentservice.controller;
import com.movento.contentservice.model.Content;
import com.movento.contentservice.model.Movie;
import com.movento.contentservice.model.TvShow;
import com.movento.contentservice.repository.ContentRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.text.Normalizer;
import java.util.Locale;

@RestController @RequestMapping("/api/v1/admin/catalog") @RequiredArgsConstructor
public class AdminCatalogController {
 private final ContentRepository content;
 @PostMapping @Transactional public ResponseEntity<Long> create(@Valid @RequestBody AdminTitleRequest request){ Content item="SERIES".equals(request.type())?new TvShow():new Movie(); apply(item,request); item.setActive(false); item=content.save(item); return ResponseEntity.created(URI.create("/api/v1/admin/catalog/"+item.getId())).body(item.getId()); }
 @PutMapping("/{id}") @Transactional public void update(@PathVariable Long id,@Valid @RequestBody AdminTitleRequest request){ apply(content.findById(id).orElseThrow(),request); }
 @PostMapping("/{id}/publish") @Transactional public void publish(@PathVariable Long id){ content.findById(id).orElseThrow().setActive(true); }
 @PostMapping("/{id}/unpublish") @Transactional public void unpublish(@PathVariable Long id){ content.findById(id).orElseThrow().setActive(false); }
 @DeleteMapping("/{id}") public ResponseEntity<Void> delete(@PathVariable Long id){ content.deleteById(id); return ResponseEntity.noContent().build(); }
 private void apply(Content item,AdminTitleRequest r){ item.setTitle(r.title().trim()); item.setSlug(r.slug()==null||r.slug().isBlank()?slug(r.title()):r.slug()); item.setDescription(r.synopsis()); item.setReleaseYear(r.releaseYear()); item.setContentRating(r.maturityRating()); item.setDurationMinutes(r.runtimeMinutes()); item.setThumbnailUrl(r.artworkUrl()); item.setBackdropUrl(r.backdropUrl()); item.setFeatured(r.featured()); item.setTrending(r.trending()); }
 private String slug(String value){ return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","").toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+","-").replaceAll("(^-|-$)",""); }
 public record AdminTitleRequest(@NotBlank String title,String slug,@NotBlank String type,String synopsis,Integer releaseYear,String maturityRating,Integer runtimeMinutes,String artworkUrl,String backdropUrl,boolean featured,boolean trending){}
}
