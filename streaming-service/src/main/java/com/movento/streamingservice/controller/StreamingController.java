package com.movento.streamingservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
public class StreamingController {

    @GetMapping
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("streaming-service up");
    }
}
