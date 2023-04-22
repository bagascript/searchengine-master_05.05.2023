package searchengine.config;

import lombok.Data;

@Data
public class Page
{
    private String path;
    private int code;
    private String content;
}
