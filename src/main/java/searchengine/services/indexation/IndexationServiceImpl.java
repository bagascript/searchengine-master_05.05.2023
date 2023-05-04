package searchengine.services.indexation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.*;
import searchengine.dto.indexation.IndexingResponse;
import searchengine.dto.indexation.SiteRunnable;
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
@Slf4j
public class IndexationServiceImpl implements IndexationService {
    private static final List<SiteEntity> siteEntities = new ArrayList<>();
    private static final String errorMsg = "Главная страница сайта не доступна";

    private final SitesList sites;
    private ExecutorService executorService = null;

    @Autowired
    private final SiteRepository siteRepository;

    @Autowired
    private final PageRepository pageRepository;

    @Override
    public IndexingResponse indexingStatusResponse() {
        cleanDataBeforeIndexing();

        for (Site site : sites.getSites()) {
            SiteEntity siteEntity = new SiteEntity();
            setIndexingStatusSite(siteEntity, site);
            siteEntities.add(siteEntity);
            if(!isValidURL(siteEntity.getUrl())) {
                setFailedStatusSite(siteEntity);
            } else {
                synchronized (Executors.class) {
                    if(executorService == null) {
                        executorService = Executors.newCachedThreadPool();
                    }
                    executorService.submit(new SiteRunnable(siteEntity, siteRepository, pageRepository));
                }
            }
        }

        return indexingResponse();
    }

    // очистка перед индексацией
    private void cleanDataBeforeIndexing() {
        List<PageEntity> pagesDelete = pageRepository.findAll();
        pageRepository.deleteAll(pagesDelete);
        List<SiteEntity> sitesDelete = siteRepository.findAll();
        siteRepository.deleteAll(sitesDelete);
    }

    // проверка доступности главной ссылки
    private boolean isValidURL(String url) {
        UrlValidator validator = new UrlValidator();
        return validator.isValid(url);
    }

    private void setIndexingStatusSite(SiteEntity siteEntity, Site site) {
        siteEntity.setName(site.getName());
        siteEntity.setUrl(site.getUrl());
        siteEntity.setStatus(StatusType.INDEXING);
        siteEntity.setLastError(null);
        siteEntity.setStatusTime(LocalDateTime.now());
        siteRepository.save(siteEntity);
    }

    private void setFailedStatusSite(SiteEntity siteEntity) {
        siteEntity.setLastError(null);
        siteEntity.setUrl(siteEntity.getUrl());
        siteEntity.setName(siteEntity.getName());
        siteRepository.updateStatusTime(siteEntity.getId());
        siteRepository.updateOnFailed(siteEntity.getId(), StatusType.FAILED, errorMsg);
        siteRepository.save(siteEntity);
    }

    // Не знаю точно при каких условиях может быть выкинут result = false с текстом 'Индексация уже запущена',
    // но в любом случае хотя бы метод создал. Будет true, если по итогу число метода count() элементов листа
    // будет совпадать с размером этой же коллекции, то есть все элементы будет иметь статус INDEXING.
    // Но, чтобы кнопка поменялась c startIndexing на stopIndexing, я поставил '!' при вызове данного метода
    // в indexingResponse(), что делать? Оставить или всё-таки
    // надо какое-то заурядное условие прописать, но какое, не подскажете? =)
    private boolean isIndexingRunning() {
        return siteEntities.stream().map(s -> s.getStatus().equals(StatusType.INDEXING)).count()
                == siteEntities.size();
    }


    private IndexingResponse indexingResponse() {
        IndexingResponse response = new IndexingResponse();
        String error;
        boolean result;
        if (!isIndexingRunning()) {
            response.setResult(false);
            response.setError("Индексация уже запущена");
        } else {
            response.setResult(true);
            response.setError(null);
        }

        error = response.getError();
        result = response.isResult();
        response.setError(error);
        response.setResult(result);
        return response;
    }
}
