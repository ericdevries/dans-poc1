package nl.knaw.dans.export;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ExportDataset {

    private String identifier;
    private List<String> creators;
    private List<String> contacts;
    private List<ExportDatasetAuthor> authors;
    private String title;
    private List<String> publishers;
    private String publicationYear;
    private String description;
    private String descriptionPlainText;
}
