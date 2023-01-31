package nl.knaw.dans.poc.export;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ExportDatasetAuthor {
    private String name;
    private String idType;
    private String idValue;
    private String affiliation;
}
