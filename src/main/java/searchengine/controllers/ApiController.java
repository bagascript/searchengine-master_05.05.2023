package searchengine.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import searchengine.dto.indexation.IndexingResponse;
import searchengine.dto.statistics.StatisticsResponse;

import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.StatusType;
import searchengine.model.repository.SiteRepository;
import searchengine.services.indexation.IndexationService;
import searchengine.services.indexation.IndexationServiceImpl;
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
    private final SiteRepository siteRepository;

    @GetMapping("/statistics")
    public ResponseEntity<StatisticsResponse> statistics() {
        return ResponseEntity.ok(statisticsService.getStatistics());
    }

    @GetMapping("/startIndexing")
    public ResponseEntity<IndexingResponse> startIndexing() {
        IndexingResponse response = new IndexingResponse();
        String error;
        boolean result;
        if(!siteRepository.selectSiteStatus()){
            response.setResult(true);
            response.setError(null);
        } else {
            response.setResult(false);
            response.setError(siteRepository.selectLastError());
        }
        error = response.getError();
        result = response.isResult();
        response.setError(error);
        response.setResult(result);
        indexationService.indexingStatusResponse();
        return ResponseEntity.ok(response);
    }
}
