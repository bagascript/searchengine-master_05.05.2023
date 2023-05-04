package searchengine.config;

import lombok.Data;
import searchengine.model.entity.SiteEntity;

@Data
public class Page
{
    private SiteEntity site;
    private String path;
    private int code;
    private String content;
}
