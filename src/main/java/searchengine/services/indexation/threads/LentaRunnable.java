package searchengine.services.indexation.threads;

import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;
import searchengine.services.indexation.IndexationServiceImpl;
import searchengine.dto.indexation.LinkFinder;
import searchengine.config.Page;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

public class LentaRunnable implements Runnable
{
    private final List<Page> lentaPagesList = new ArrayList<>();

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    private String url;

    private SiteEntity siteEntity;

    public LentaRunnable(SiteRepository siteRepository, PageRepository pageRepository,
                            String url, SiteEntity siteEntity) {
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.url = url;
        this.siteEntity = siteEntity;
    }

    @Override
    public void run() {
        synchronized (IndexationServiceImpl.getLock()) {
            LinkFinder linkFinder = new LinkFinder(url, siteEntity.getId(), siteRepository);
            ForkJoinPool forkJoinPool = new ForkJoinPool();
            forkJoinPool.invoke(linkFinder);
            forkJoinPool.shutdown();
            Set<Page> pages = linkFinder.getPages();
            lentaPagesList.addAll(pages);
        }
    }

    public void getLentaPages(){
        for(Page lentaPage : lentaPagesList) {
            PageEntity lentaPageEntity = new PageEntity();
            lentaPageEntity.setPath(lentaPage.getPath());
            lentaPageEntity.setContent(lentaPage.getContent());
            lentaPageEntity.setCode(lentaPage.getCode());
            lentaPageEntity.setSite(siteEntity);
            pageRepository.save(lentaPageEntity);
        }
    }
}
