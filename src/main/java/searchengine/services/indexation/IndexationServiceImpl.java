package searchengine.services.indexation;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.indexation.LinkFinder;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.StatusType;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
public class IndexationServiceImpl implements IndexationService {
    private static Set<PageEntity> checkOnFailedPages = new HashSet<>();
    private static Set<PageEntity> pages = new HashSet<>();
    private final SitesList sites;
    private List<SiteEntity> siteEntities = new ArrayList<>();

    @Autowired
    private final SiteRepository siteRepository;

    @Autowired
    private final PageRepository pageRepository;

    @Override
    public void indexingStatusResponse() {
        ExecutorService executorService = Executors.newCachedThreadPool();
        List<PageEntity> pagesDelete = pageRepository.findAll();
        pageRepository.deleteAll(pagesDelete);
        List<SiteEntity> sitesDelete = siteRepository.findAll();
        siteRepository.deleteAll(sitesDelete);

        List<Site> siteList = sites.getSites();
        for (Site site : siteList) {
            SiteEntity siteEntity = new SiteEntity();
            siteEntity.setName(site.getName());
            siteEntity.setUrl(site.getUrl());
            siteEntity.setStatus(StatusType.INDEXING);
            siteEntity.setLastError(null);
            siteEntity.setStatusTime(LocalDateTime.now());
            siteRepository.save(siteEntity);

            executorService.submit(() -> {
                LinkFinder linkFinder = new LinkFinder(siteEntity.getUrl(), siteEntity.getId(),
                        siteRepository, pageRepository, siteEntity);
                ForkJoinPool forkJoinPool = new ForkJoinPool();
                forkJoinPool.invoke(linkFinder);
                forkJoinPool.shutdown();
                pages.addAll(linkFinder.getPages());
                for (PageEntity page : pages) {
                    if (String.valueOf(page.getCode()).startsWith("4") ||
                            String.valueOf(page.getCode()).startsWith("5")) {
                        siteEntity.setStatus(StatusType.FAILED);
                        siteEntity.setLastError("Сайт не доступен");
                        siteRepository.updateOnFailed(siteEntity.getId(),
                                siteEntity.getStatus(), siteEntity.getLastError());
                        siteRepository.updateStatusTime(siteEntity.getId());

                    } else {
                        checkOnFailedPages.add(page);
                    }
                }
                if (checkOnFailedPages.size() == pages.size()) {
                    siteEntity.setStatus(StatusType.INDEXED);
                    siteEntity.setLastError(null);
                    siteRepository.updateOnIndexed(siteEntity.getId(),
                            siteEntity.getStatus());
                    siteRepository.updateStatusTime(siteEntity.getId());
                }
                siteEntities.add(siteEntity);
            });
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }
}
