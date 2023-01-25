package nl.knaw.dans.export;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString(callSuper = true)
public class PrimitiveExportMetadataField extends ExportMetadataField {
    private List<String> values;
}
