package searchengine.dto.indexation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import searchengine.config.Page;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.StatusType;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;

@Slf4j
@RequiredArgsConstructor
public class SiteRunnable implements Runnable {
    private static final Object LOCK = new Object();
    private final SiteEntity site;
    private final SiteRepository siteRepository;
    private final PageRepository pageRepository;

    @Override
    public void run() {
        LinkFinder linkFinder = new LinkFinder(site.getUrl(), site.getId(), siteRepository, site);
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        ConcurrentHashMap<String, SiteEntity> links = forkJoinPool.invoke(linkFinder);
        ConcurrentHashMap<Page, SiteEntity> pages = linkFinder.getPages();
        forkJoinPool.shutdown();

        // Добавление данных Page в БД PageEntity
        for (Map.Entry<Page, SiteEntity> entry : pages.entrySet()) {
            if (!pageRepository.existsByPath(entry.getKey().getPath())) {
                PageEntity pageEntity = new PageEntity();
                pageEntity.setSite(entry.getValue());
                pageEntity.setPath(entry.getKey().getPath());
                pageEntity.setContent(entry.getKey().getContent());
                pageEntity.setCode(entry.getKey().getCode());

                synchronized (LOCK) {
                    pageRepository.saveAndFlush(pageEntity);
                }
            }
        }

            // Поиск последней сохраненной ссылки сайта в БД и если true - установка статуса INDEXED
            for (Map.Entry<String, SiteEntity> entry : links.entrySet()) {
                if (pageRepository.getLastSitePage(entry.getValue()).equals(entry.getKey())) {
                    SiteEntity siteEntity = siteRepository.findByNameAndStatus(entry.getValue().getName(),
                            StatusType.INDEXING).orElse(site);
                    siteEntity.setStatusTime(LocalDateTime.now());
                    saveSiteData(siteEntity);
                    break;
                }
            }
        }

        // Передача обновления статуса с INDEXING на INDEXED
        private void saveSiteData (SiteEntity site){
            siteRepository.updateOnIndexed(site.getId(), StatusType.INDEXED);
        }
    }