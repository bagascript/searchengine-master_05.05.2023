package searchengine.services.lemmatisation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.Site;
import searchengine.config.SitesList;

import searchengine.dto.lemmatisation.LemmaResponse;
import searchengine.dto.lemmatisation.LemmaService;

import searchengine.model.entity.IndexEntity;

import searchengine.model.entity.PageEntity;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.io.IOException;

import java.util.Map;

import java.util.Optional;

import static searchengine.dto.lemmatisation.LemmaServiceImpl.indexIdCollection;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexPageServiceImpl implements IndexPageService
{
    private static final String INVALID_PAGE = "Данная страница находится за пределами сайтов, указанных в конфигурационном файле";

    private LemmaResponse lemmaResultResponse = new LemmaResponse();

    private final SitesList sites;

    @Autowired
    private final SiteRepository siteRepository;

    @Autowired
    private final PageRepository pageRepository;

    @Autowired
    private final IndexRepository indexRepository;


    @Autowired
    private final LemmaService lemmaService;

    @Override
    public LemmaResponse indexPageResponse(String path) {
        deletePageData(path);
        try {
            Thread.sleep(1000);
            Connection.Response connection = Jsoup.connect(path)
                    .userAgent("Mozilla")
                    .referrer("http://www.google.com")
                    .execute();
            Document document = connection.parse();
            String title = document.title();
            for(Site site : sites.getSites()) {
                if(path.startsWith(site.getUrl())) {
                    PageEntity pageEntity = new PageEntity();
                    pageEntity.setPath(path);
                    pageEntity.setCode(connection.statusCode());
                    pageEntity.setContent(title);
                    pageEntity.setSite(siteRepository.getSiteId(site.getUrl()));
                    pageRepository.save(pageEntity);
                    lemmaService.filterPageContent(pageRepository.getContent(path), pageEntity);
                    lemmaResultResponse = sendLemmaResponse(null, true);
                } else {
                    lemmaResultResponse = sendLemmaResponse(INVALID_PAGE, false);
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
        return lemmaResultResponse;
    }

    private void deletePageData(String path) {
        if(pageRepository.existsByPath(path)) {
            PageEntity pageEntity = pageRepository.findByPath(path);
            for(Map.Entry<Integer, PageEntity> entry : indexIdCollection.entrySet()) {
                if (indexRepository.existsById(entry.getKey())) {
                    Optional<IndexEntity> indexEntity = indexRepository.findById(entry.getKey());
                    if (indexEntity.get().getPage().equals(pageEntity)) {
                        indexRepository.deleteId(entry.getKey());
                    }
                }
            }
            lemmaService.deleteLemmas(pageEntity.getContent(), pageEntity);
            pageRepository.delete(pageEntity);
        }
    }

    private LemmaResponse sendLemmaResponse(String error, boolean result) {
        LemmaResponse lemmaResponse = new LemmaResponse();
        lemmaResponse.setResult(result);
        lemmaResponse.setError(error);
        return lemmaResponse;
    }
}
