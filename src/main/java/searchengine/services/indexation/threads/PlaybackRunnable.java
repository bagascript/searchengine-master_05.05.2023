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

public class PlaybackRunnable implements Runnable
{
    private final List<Page> playbackPagesList = new ArrayList<>();

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    private String url;

    private SiteEntity siteEntity;

    public PlaybackRunnable(SiteRepository siteRepository, PageRepository pageRepository,
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
            playbackPagesList.addAll(pages);
        }
    }

    public void getPlaybackPages(){
        for(Page playbackPage : playbackPagesList) {
            PageEntity playbackPageEntity = new PageEntity();
            playbackPageEntity.setPath(playbackPage.getPath());
            playbackPageEntity.setContent(playbackPage.getContent());
            playbackPageEntity.setCode(playbackPage.getCode());
            playbackPageEntity.setSite(siteEntity);
            pageRepository.save(playbackPageEntity);
        }
    }
}
