package com.movento.contentservice.controller;
import com.movento.contentservice.model.PlaybackProgress;
import com.movento.contentservice.model.WatchlistItem;
import com.movento.contentservice.repository.ContentRepository;
import com.movento.contentservice.repository.PlaybackProgressRepository;
import com.movento.contentservice.repository.WatchlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController @RequestMapping("/api/v1/library") @RequiredArgsConstructor
public class LibraryController {
 private final WatchlistRepository watchlist; private final PlaybackProgressRepository progress; private final ContentRepository content;
 @GetMapping("/watchlist") public List<Long> watchlist(@RequestHeader("X-Profile-Id") UUID profileId) { return watchlist.findByProfileIdOrderByCreatedAtDesc(profileId).stream().map(i->i.getContent().getId()).toList(); }
 @PutMapping("/watchlist/{contentId}") @Transactional public void add(@RequestHeader("X-Profile-Id") UUID profileId,@PathVariable Long contentId) { if(watchlist.findByProfileIdAndContentId(profileId,contentId).isEmpty()){ WatchlistItem item=new WatchlistItem(); item.setProfileId(profileId); item.setContent(content.findById(contentId).orElseThrow()); watchlist.save(item); } }
 @DeleteMapping("/watchlist/{contentId}") @Transactional public void remove(@RequestHeader("X-Profile-Id") UUID profileId,@PathVariable Long contentId){ watchlist.findByProfileIdAndContentId(profileId,contentId).ifPresent(watchlist::delete); }
 @GetMapping("/progress") public List<Map<String,Object>> progress(@RequestHeader("X-Profile-Id") UUID profileId){ return progress.findTop20ByProfileIdOrderByUpdatedAtDesc(profileId).stream().map(p->Map.<String,Object>of("contentId",p.getContent().getId(),"progressSeconds",p.getProgressSeconds(),"durationSeconds",p.getDurationSeconds(),"completed",p.isCompleted())).toList(); }
 @PutMapping("/progress/{contentId}") @Transactional public void update(@RequestHeader("X-Profile-Id") UUID profileId,@PathVariable Long contentId,@RequestBody ProgressRequest request){ String episode=request.episodeKey()==null?"":request.episodeKey(); PlaybackProgress item=progress.findByProfileIdAndContentIdAndEpisodeKey(profileId,contentId,episode).orElseGet(PlaybackProgress::new); item.setProfileId(profileId); item.setContent(content.findById(contentId).orElseThrow()); item.setEpisodeKey(episode); item.setProgressSeconds(Math.max(0,request.progressSeconds())); item.setDurationSeconds(Math.max(1,request.durationSeconds())); item.setCompleted(item.getProgressSeconds()>=(int)(item.getDurationSeconds()*.9)); item.setUpdatedAt(Instant.now()); progress.save(item); }
 public record ProgressRequest(int progressSeconds,int durationSeconds,String episodeKey){}
}
