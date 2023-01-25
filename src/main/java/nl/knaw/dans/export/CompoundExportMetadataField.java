package nl.knaw.dans.export;

import lombok.Data;
import lombok.ToString;

import java.util.List;

@Data
@ToString(callSuper = true)
public class CompoundExportMetadataField extends ExportMetadataField {

    private List<List<ExportMetadataField>> values;
}
