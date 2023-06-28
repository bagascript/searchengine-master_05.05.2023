package searchengine.dto.lemmatisation;

import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.PageEntity;

import java.util.HashMap;

public interface IndexService {
    void indexLemmaQuantityForPage(PageEntity pageEntity, HashMap<LemmaEntity, Integer> resultMapForIndexEntity);
}
