package searchengine.dto.lemmatisation;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.LemmaEntity;
import searchengine.model.entity.PageEntity;
import searchengine.model.repository.IndexRepository;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class IndexServiceImpl implements IndexService
{

    @Autowired
    private final IndexRepository indexRepository;

    @Override
    public void indexLemmaQuantityForPage(PageEntity pageEntity, HashMap<LemmaEntity, Integer> resultMapForIndexEntity) {
        for(Map.Entry<LemmaEntity, Integer> entry : resultMapForIndexEntity.entrySet()) {
            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setRank(entry.getValue());
            indexEntity.setLemma(entry.getKey());
            indexEntity.setPage(pageEntity);
            indexRepository.save(indexEntity);

        }
    }
}
