package nl.knaw.dans.export;

import lombok.Data;

@Data
public class ExportMetadataField {
    private ExportMetadataNamespace namespace;
    private String name;
}
