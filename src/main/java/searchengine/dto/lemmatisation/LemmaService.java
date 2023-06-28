package searchengine.dto.lemmatisation;

import searchengine.model.entity.PageEntity;

public interface LemmaService {
    void filterPageContent(String content, PageEntity pageEntity);

    void deleteLemmas(String content, PageEntity pageEntity);
}
