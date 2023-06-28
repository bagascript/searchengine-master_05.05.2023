package searchengine.controllers;

import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import searchengine.dto.indexation.IndexingResponse;
import searchengine.dto.lemmatisation.LemmaResponse;
import searchengine.dto.statistics.StatisticsResponse;

import searchengine.services.indexation.IndexationService;
import searchengine.services.lemmatisation.IndexPageService;
import searchengine.services.statistics.StatisticsService;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiController
{
    @Autowired
    private final IndexationService indexationService;

    @Autowired
    private final StatisticsService statisticsService;

    @Autowired
    private final IndexPageService indexPageService;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        return ResponseEntity.ok(indexationService.indexingStatusResponse());
    }

    @GetMapping("/stopIndexing")
    public ResponseEntity<IndexingResponse> stopIndexing() {
        return ResponseEntity.ok(indexationService.stoppingStatusResponse());
    }

    @PostMapping(value = "/indexPage")
    public ResponseEntity<LemmaResponse> indexPage(@RequestBody String path) {
        return ResponseEntity.ok(indexPageService.indexPageResponse(path));
    }
}
