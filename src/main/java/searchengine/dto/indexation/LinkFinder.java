package searchengine.dto.indexation;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.entity.PageEntity;
import searchengine.model.entity.SiteEntity;
import searchengine.model.entity.StatusType;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;
import searchengine.config.Page;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class LinkFinder extends RecursiveTask<Set<String>> {
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    private SiteEntity siteEntity;

    private static Set<PageEntity> pages = new HashSet<>();
    private final String url;
    private final int id;
    private static volatile Set<String> checkUrl = new HashSet<>();


    public LinkFinder(String url, int id, SiteRepository siteRepository,
                      PageRepository pageRepository, SiteEntity siteEntity) {
        this.id = id;
        this.url = url.trim();
        this.siteRepository = siteRepository;
        this.pageRepository = pageRepository;
        this.siteEntity = siteEntity;
    }

    @Override
    protected Set<String> compute() {
        Set<String> links = new HashSet<>();
        Set<LinkFinder> tasks = new HashSet<>();
        links.add(url);
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
                        && !checkUrl.contains(item)
                        && !item.contains("#")
                        && !item.contains("?")) {
                    String content = el.html();

                    PageEntity pageEntity = new PageEntity();
                    pageEntity.setSite(siteEntity);
                    pageEntity.setPath(item);
                    pageEntity.setContent(content);
                    pageEntity.setCode(response.statusCode());
                    siteRepository.updateStatusTime(id);
                    pages.add(pageEntity);
                    pageRepository.save(pageEntity);

                    LinkFinder linkFinderTask = new LinkFinder(item, id, siteRepository, pageRepository,siteEntity);
                    linkFinderTask.fork();
                    tasks.add(linkFinderTask);
                    checkUrl.add(item);
                    System.out.println(checkUrl.size());
                }
            });
        } catch (InterruptedException | IOException ignored) {
        }

        for (LinkFinder task : tasks) {
            links.addAll(task.join());
        }
        return links;
    }

    public Set<PageEntity> getPages(){
        return pages;
    }
}

