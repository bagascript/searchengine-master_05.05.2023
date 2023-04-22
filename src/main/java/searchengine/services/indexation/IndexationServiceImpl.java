package searchengine.services.indexation;

import lombok.RequiredArgsConstructor;
import org.apache.commons.validator.routines.UrlValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.indexation.IndexingResponse;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.StatusType;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;
import searchengine.services.indexation.threads.LentaRunnable;
import searchengine.services.indexation.threads.PlaybackRunnable;
import searchengine.services.indexation.threads.SkillboxRunnable;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IndexationServiceImpl implements IndexationService
{
    private static final Object lock = new Object();
    private final SitesList sites;

    @Autowired
    private final SiteRepository siteRepository;

    @Autowired
    private final PageRepository pageRepository;

    @Override
    public IndexingResponse indexingStatusResponse()  {
        IndexingResponse response;
        List<SiteEntity> siteEntities = new ArrayList<>();

        List<PageEntity> pagesDelete = pageRepository.findAll();
        pageRepository.deleteAll(pagesDelete);
        List<SiteEntity> sitesDelete = siteRepository.findAll();
        siteRepository.deleteAll(sitesDelete);

        Site playbackSite = sites.getSites().get(0);

        SiteEntity playbackSiteEntity = new SiteEntity();
        playbackSiteEntity.setName(playbackSite.getName());
        playbackSiteEntity.setUrl(playbackSite.getUrl());
        playbackSiteEntity.setStatus(StatusType.INDEXING);
        playbackSiteEntity.setStatusTime(LocalDateTime.now());
        playbackSiteEntity.setLastError(null);
        siteRepository.save(playbackSiteEntity);

        siteEntities.add(playbackSiteEntity);

        Site lentaSite = sites.getSites().get(1);

        SiteEntity lentaSiteEntity = new SiteEntity();
        lentaSiteEntity.setName(lentaSite.getName());
        lentaSiteEntity.setUrl(lentaSite.getUrl());
        lentaSiteEntity.setStatus(StatusType.INDEXING);
        lentaSiteEntity.setStatusTime(LocalDateTime.now());
        lentaSiteEntity.setLastError(null);
        siteRepository.save(lentaSiteEntity);

        siteEntities.add(lentaSiteEntity);

        Site skillboxSite = sites.getSites().get(2);

        SiteEntity skillboxSiteEntity = new SiteEntity();
        skillboxSiteEntity.setName(skillboxSite.getName());
        skillboxSiteEntity.setUrl(skillboxSite.getUrl());
        skillboxSiteEntity.setStatus(StatusType.INDEXING);
        skillboxSiteEntity.setStatusTime(LocalDateTime.now());
        skillboxSiteEntity.setLastError(null);
        siteRepository.save(skillboxSiteEntity);

        siteEntities.add(skillboxSiteEntity);

        PlaybackRunnable playbackRunnable = new PlaybackRunnable(siteRepository, pageRepository,
                playbackSite.getUrl(), playbackSiteEntity);
        LentaRunnable lentaRunnable = new LentaRunnable(siteRepository, pageRepository,
                lentaSite.getUrl(), lentaSiteEntity);
        SkillboxRunnable skillboxRunnable = new SkillboxRunnable(siteRepository, pageRepository,
                skillboxSite.getUrl(), skillboxSiteEntity);
        Thread playbackThread = new Thread(playbackRunnable);
        Thread lentaThread = new Thread(lentaRunnable);
        Thread skillboxThread = new Thread(skillboxRunnable);
        playbackThread.start();
        lentaThread.start();
        skillboxThread.start();
        try {
            playbackThread.join();
            lentaThread.join();
            skillboxThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        playbackRunnable.getPlaybackPages();
        lentaRunnable.getLentaPages();
        skillboxRunnable.getSkillboxPages();

        response = getResponse(siteEntities);

        return response;
    }

    private IndexingResponse getResponse(List<SiteEntity> siteEntities){
        IndexingResponse indexingResponse = new IndexingResponse();
        for(SiteEntity siteEntity : siteEntities){
            if(pageRepository.checkStatusCodePage(siteEntity) == 1){
                siteEntity.setStatus(StatusType.INDEXED);
                siteRepository.updateOnIndexed(siteEntity.getId(), StatusType.INDEXED);
            } else {
                UrlValidator urlValidator = new UrlValidator();
                if(!urlValidator.isValid(siteEntity.getUrl())) {
                    siteEntity.setLastError("Главная страница сайта не доступна");
                } else {
                    siteEntity.setLastError("Сайт не доступен");
                }
                siteEntity.setStatus(StatusType.FAILED);
                siteRepository.updateOnFailed(siteEntity.getId(),
                        StatusType.FAILED, siteEntity.getLastError());
            }

            boolean formatResponse;
            String getError;
            if(siteEntity.getStatus().name().equals("INDEXED")) {
                indexingResponse.setResult(true);
                indexingResponse.setError(null);
            } else if(siteEntity.getStatus().name().equals("INDEXING")){
                indexingResponse.setResult(false);
                indexingResponse.setError("Индексация уже запущена");
            } else {
                indexingResponse.setResult(false);
                indexingResponse.setError(siteEntity.getLastError());
            }
            formatResponse = indexingResponse.isResult();
            getError = indexingResponse.getError();
            indexingResponse.setResult(formatResponse);
            indexingResponse.setError(getError);
        }
        return indexingResponse;
    }

    public static Object getLock() {
        return lock;
    }
}
