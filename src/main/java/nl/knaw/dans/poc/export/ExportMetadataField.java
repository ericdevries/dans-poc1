package nl.knaw.dans.poc.export;

import lombok.Data;

@Data
public class ExportMetadataField {
    private ExportMetadataNamespace namespace;
    private String name;
}
