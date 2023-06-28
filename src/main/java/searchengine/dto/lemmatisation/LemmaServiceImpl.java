package searchengine.dto.lemmatisation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import searchengine.config.Site;
import searchengine.config.SitesList;
import searchengine.model.entity.IndexEntity;
import searchengine.model.entity.LemmaEntity;

import searchengine.model.entity.PageEntity;
import searchengine.model.repository.IndexRepository;
import searchengine.model.repository.LemmaRepository;
import searchengine.model.repository.PageRepository;
import searchengine.model.repository.SiteRepository;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LemmaServiceImpl implements LemmaService {
    private static final HashMap<String, Integer> lemmaTotalMap = new HashMap<>();
    private static final HashMap<String, Integer> lemmaMapForOnePage = new HashMap<>();
    private static final HashMap<Integer, String> lemmaIdCollection = new HashMap<>();
    public static final HashMap<Integer, PageEntity> indexIdCollection = new HashMap<>();

    private static final int lemmaTotalCounter = 1;
    private static final int lemmaCounterForOnePage = 1;
    private static final String regex = "[^а-яА-Яa-zA-Z\\s]";
    private static final String[] functionalTypes = new String[] {"МЕЖД", "ПРЕДЛ", "СОЮЗ", "ЧАСТ",
                    "CONJ", "PREP", "ARTICLE", "PART", "INT"};

    private final SitesList sites;

    @Autowired
    private final PageRepository pageRepository;

    @Autowired
    private final SiteRepository siteRepository;

    @Autowired
    private final LemmaRepository lemmaRepository;

    @Autowired
    private final IndexRepository indexRepository;


    //TODO: IntValue() is null - в мапах по всему классу нулевые значения, вай?
    @Override
    public void filterPageContent(String content, PageEntity pageEntity) {
        String result = content.replaceAll("<(.*?)+>", "").trim();

        if (!result.isEmpty()) {
            saveLemmaAndIndexData(convertContentToLemmas(result), content, pageEntity);
        }
    }

    @Override
    public void deleteLemmas(String content, PageEntity pageEntity) {
        for(String word : splitContentIntoWords(content)) {
            String resultWordForm = editLemma(returnWordIntoBaseForm(word));
            LemmaEntity lemmaEntity = lemmaRepository.getLemma(resultWordForm);
            checkLemmaOnDeleteOrUpdateData(resultWordForm, lemmaEntity);
        }
    }

    private void checkLemmaOnDeleteOrUpdateData(String resultWordForm, LemmaEntity lemmaEntity) {
        if(lemmaTotalMap.get(resultWordForm) > 1) {
            int decrementLemmaCounter = lemmaTotalMap.get(resultWordForm) - 1;
            lemmaRepository.updateFrequency(lemmaEntity.getId(), decrementLemmaCounter);
            lemmaTotalMap.put(resultWordForm, decrementLemmaCounter);
        } else if(lemmaTotalMap.get(resultWordForm) == 1){
            lemmaRepository.deleteById(lemmaEntity.getId());
            lemmaTotalMap.remove(resultWordForm);
            System.out.println(lemmaTotalMap);
        }
    }

    private HashMap<String, Integer> convertContentToLemmas(String content) {
        String [] words = splitContentIntoWords(content);
        for (String word : words) {
            List<String> wordBaseForms = returnWordIntoBaseForm(word);
            if (wordBaseForms.isEmpty()) {
                continue;
            }
            if (isWordFunctional(wordBaseForms)) {
                continue;
            }

            String resultWordForm = editLemma(wordBaseForms);

            if (!lemmaMapForOnePage.containsKey(resultWordForm)) {
                lemmaMapForOnePage.put(resultWordForm, lemmaCounterForOnePage);
            } else {
                int incrementLemmaCounter = lemmaMapForOnePage.get(resultWordForm) + 1;
                lemmaMapForOnePage.put(resultWordForm, incrementLemmaCounter);
            }


            lemmaTotalMap.putAll(lemmaDataIntoMap(resultWordForm));
        }
        return lemmaTotalMap;
    }

    private HashMap<String, Integer> lemmaDataIntoMap(String resultWordForm) {
        if (!lemmaTotalMap.containsKey(resultWordForm)) {
            lemmaTotalMap.put(resultWordForm, lemmaTotalCounter);
        } else {
            int incrementLemmaCounter = lemmaTotalMap.get(resultWordForm) + 1;
            lemmaTotalMap.put(resultWordForm, incrementLemmaCounter);
        }
        return lemmaTotalMap;
    }

    private String[] splitContentIntoWords(String content) {
        String[] words;

        if (content.substring(0, 1).matches(regex)) {
            words = content.replace(content.substring(0, 1), "")
                    .replaceAll(regex, " ").toLowerCase().split("\\s+");
        } else {
            words = content.replaceAll(regex, " ").toLowerCase().split("\\s+");
        }

        return words;
    }

    private List<String> returnWordIntoBaseForm(String word) {
        LuceneMorphology luceneMorph;
        try {
            if (word.matches("[а-яА-Я]+")) {
                luceneMorph = new RussianLuceneMorphology();
            } else {
                luceneMorph = new EnglishLuceneMorphology();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return luceneMorph.getMorphInfo(word);
    }

    private String editLemma(List<String> wordBaseForms) {
        int slashIndex = wordBaseForms.get(wordBaseForms.size() - 1).indexOf("|");
        return wordBaseForms.get(wordBaseForms.size() - 1).substring(0, slashIndex);
    }

    private boolean isWordFunctional(List<String> wordBaseForms) {
        boolean result = false;
        for (String functionalType : functionalTypes) {
            if (wordBaseForms.get(0).contains(functionalType)) {
                result = true;
                break;
            }
        }
        return result;
    }

    //TODO: проблема в этом методе :)
    private void saveLemmaAndIndexData(HashMap<String, Integer> lemmaMap, String content, PageEntity pageEntity) {
        if (!lemmaMap.isEmpty()) {
            for (Map.Entry<String, Integer> entry : lemmaMap.entrySet()) {
                HashMap<LemmaEntity, Integer> resultMapForIndexEntity = new HashMap<>();
                if(lemmaRepository.existsByLemma(entry.getKey()) ) {
                    for(Map.Entry<Integer, String> entry2 : lemmaIdCollection.entrySet()) {
                        if(entry2.getValue().equals(entry.getKey())) {
                            lemmaRepository.updateFrequency(entry2.getKey(), entry.getValue());
                        }
                    }
                    if(!lemmaMapForOnePage.containsKey(entry.getKey())) {
                        continue;
                    }
                    LemmaEntity lemmaEntity = lemmaRepository.getLemma(entry.getKey());
                    resultMapForIndexEntity.put(lemmaEntity, lemmaMapForOnePage.get(lemmaEntity.getLemma()));
                    indexLemmaQuantityForPage(pageEntity, resultMapForIndexEntity);
                } else {
                    LemmaEntity lemmaEntity = new LemmaEntity();
                    lemmaEntity.setLemma(entry.getKey());
                    lemmaEntity.setFrequency(entry.getValue());
                    String pagePath = pageRepository.getPath(content);
                    for (Site site : sites.getSites()) {
                        if (!pagePath.startsWith(site.getUrl())) {
                            continue;
                        }
                        lemmaEntity.setSite(siteRepository.getSiteId(site.getUrl()));
                    }
                    if(!lemmaMapForOnePage.containsKey(entry.getKey())) {
                        continue;
                    }
                    lemmaRepository.save(lemmaEntity);
                    lemmaIdCollection.put(lemmaEntity.getId(), lemmaEntity.getLemma());
                    resultMapForIndexEntity.put(lemmaEntity, lemmaMapForOnePage.get(lemmaEntity.getLemma()));
                    indexLemmaQuantityForPage(pageEntity, resultMapForIndexEntity);
                }
            }
            lemmaMapForOnePage.clear();
        }
    }

    public void indexLemmaQuantityForPage(PageEntity pageEntity, HashMap<LemmaEntity, Integer> resultMapForIndexEntity) {
        for(Map.Entry<LemmaEntity, Integer> entry : resultMapForIndexEntity.entrySet()) {
            IndexEntity indexEntity = new IndexEntity();
            indexEntity.setRank(entry.getValue());
            indexEntity.setLemma(entry.getKey());
            indexEntity.setPage(pageEntity);
            indexRepository.save(indexEntity);
            indexIdCollection.put(indexEntity.getId(), pageEntity);

        }
    }
}
