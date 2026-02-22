package com.movento.contentservice.controller;

import com.movento.contentservice.dto.ViewHistoryDto;
import com.movento.contentservice.dto.request.ViewHistoryRequest;
import com.movento.contentservice.dto.response.ApiResponse;
import com.movento.contentservice.service.ViewHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.validation.Valid;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/view-history")
@RequiredArgsConstructor
public class ViewHistoryController {

    private final ViewHistoryService viewHistoryService;

    @PostMapping
    public ResponseEntity<ApiResponse<ViewHistoryDto>> addToHistory(@Valid @RequestBody ViewHistoryRequest request) {
        ViewHistoryDto createdHistory = viewHistoryService.addToHistory(request);
        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(createdHistory.getId())
                .toUri();
        return ResponseEntity.created(location).body(
                ApiResponse.<ViewHistoryDto>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("View history added successfully")
                        .data(createdHistory)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ViewHistoryDto>> getHistoryById(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.<ViewHistoryDto>builder()
                        .status(HttpStatus.OK.value())
                        .message("View history retrieved successfully")
                        .data(viewHistoryService.getHistoryById(id))
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ViewHistoryDto>>> getViewHistory(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Long contentId,
            Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.<Page<ViewHistoryDto>>builder()
                        .status(HttpStatus.OK.value())
                        .message("View history retrieved successfully")
                        .data(viewHistoryService.getViewHistory(userId, contentId, pageable))
                        .build()
        );
    }

    @GetMapping("/users/{userId}/recent")
    public ResponseEntity<ApiResponse<Page<ViewHistoryDto>>> getRecentWatched(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(
            ApiResponse.<Page<ViewHistoryDto>>builder()
                .status(HttpStatus.OK.value())
                .message("Recently watched content retrieved successfully")
                .data(viewHistoryService.getRecentWatched(userId, limit))
                .build()
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeFromHistory(@PathVariable Long id) {
        viewHistoryService.removeFromHistory(id);
        return ResponseEntity.ok(
            ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("View history removed successfully")
                .build()
        );
    }
}
