package nl.knaw.dans.export;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExportMetadataNamespace {
    private String term;
    private String uri;
}
