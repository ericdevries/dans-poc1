package nl.knaw.dans.poc.export;

import lombok.Data;
import lombok.ToString;

import javax.json.JsonObject;
import java.util.List;

@Data
@ToString(callSuper = true)
public class RawJsonExportMetadataField extends ExportMetadataField {

    private List<JsonObject> values;
}
