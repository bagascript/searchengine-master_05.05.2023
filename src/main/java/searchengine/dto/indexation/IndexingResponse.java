package searchengine.dto.indexation;

import lombok.Data;

@Data
public class IndexingResponse
{
    private boolean result;
    private String error;
}
