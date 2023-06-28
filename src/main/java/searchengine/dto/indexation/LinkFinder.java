package searchengine.dto.indexation;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.entity.SiteEntity;
import searchengine.model.repository.SiteRepository;
import searchengine.config.Page;
import searchengine.services.indexation.IndexationServiceImpl;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RecursiveTask;

public class LinkFinder extends RecursiveTask<ConcurrentHashMap<String, SiteEntity>> {
    private static ConcurrentHashMap<Page, SiteEntity> pages = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Integer> checkUrl = new ConcurrentHashMap<>();

    private final SiteEntity siteEntity;
    private final String url;
    private final int id;

    @Autowired
    private final SiteRepository siteRepository;

    public LinkFinder(String url, int id, SiteRepository siteRepository, SiteEntity siteEntity) {
        this.id = id;
        this.url = url.trim();
        this.siteRepository = siteRepository;
        this.siteEntity = siteEntity;
    }

    @Override
    protected ConcurrentHashMap<String, SiteEntity> compute() {
        ConcurrentHashMap<String, SiteEntity> links = new ConcurrentHashMap<>();
        ConcurrentHashMap<LinkFinder, SiteEntity> tasks = new ConcurrentHashMap<>();
        links.put(url, siteEntity);
        Document document;
        Elements elements;
        try {
            Thread.sleep(1000);
            Connection.Response response = Jsoup.connect(url)
                    .userAgent("Mozilla")
                    .referrer("http://www.google.com")
                    .execute();
            document = response.parse();
            elements = document.select("a");
            elements.forEach(el -> {
                String item = el.attr("abs:href");
                if (item.startsWith(url)
                        && !checkUrl.containsKey(item)
                        && !item.contains("#")
                        && !item.contains("?")) {
                    String content = el.html();
                    Page page = new Page(); // прототип PageEntity
                    page.setCode(response.statusCode());
                    page.setSite(siteEntity);
                    page.setPath(item);
                    page.setContent(content);
                    putPageDataInMap(page);
                    LinkFinder linkFinderTask = new LinkFinder(item, id, siteRepository, siteEntity);
                    linkFinderTask.fork();
                    tasks.put(linkFinderTask, siteEntity);
                    checkUrl.put(item, siteEntity.getId());
                    System.out.println(checkUrl.size());
                }
            });
        } catch (InterruptedException | IOException ignored) {
        }

        for (Map.Entry<LinkFinder, SiteEntity> entry : tasks.entrySet()) {
            links.putAll(entry.getKey().join());
        }

        return links;
    }

    private void putPageDataInMap(Page page) {
        if (!pages.containsKey(page)) {
            pages.put(page, siteEntity);
        }

        if(!IndexationServiceImpl.isCanceled) {
            siteRepository.updateStatusTime(id); // постоянное обновление status_time
        }
    }

    public ConcurrentHashMap<Page, SiteEntity> getPages() {
        return pages;
    }
}
