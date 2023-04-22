package searchengine.dto.indexation;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import searchengine.model.repository.SiteRepository;
import searchengine.config.Page;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class LinkFinder extends RecursiveTask<Set<String>> {
    @Autowired
    private SiteRepository siteRepository;
    private static volatile Set<Page> pages = new HashSet<>();
    private final String url;
    private final int id;
    private static volatile Set<String> checkUrl = new HashSet<>();


    public LinkFinder(String url, int id, SiteRepository siteRepository) {
        this.id = id;
        this.url = url.trim();
        this.siteRepository = siteRepository;
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
                    Page page = new Page();
                    page.setPath(item);
                    page.setContent(content);
                    page.setCode(response.statusCode());
                    pages.add(page);
                    LinkFinder linkFinderTask = new LinkFinder(item, id, siteRepository);
                    linkFinderTask.fork();
                    tasks.add(linkFinderTask);
                    checkUrl.add(item);
                    siteRepository.updateStatusTime(id);
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

    public Set<Page> getPages() {
        return pages;
    }
}

