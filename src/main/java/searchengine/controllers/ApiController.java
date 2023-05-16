package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import searchengine.dto.indexation.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;
import searchengine.services.indexation.IndexationService;
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
}
