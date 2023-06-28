package searchengine.services.lemmatisation;

import searchengine.dto.lemmatisation.LemmaResponse;

public interface IndexPageService
{
    LemmaResponse indexPageResponse(String url);
}
