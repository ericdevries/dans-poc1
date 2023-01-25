package nl.knaw.dans.export;

import javax.json.JsonObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MetadataFieldFactory {

    private static Map<String, ExportMetadataNamespace> namespaces = new HashMap<>();

    public static ExportMetadataNamespace getNamespace(String term, String namespace) {
        return namespaces.computeIfAbsent(term, (key) -> new ExportMetadataNamespace(key, namespace));
    }

    public static Optional<ExportMetadataNamespace> getNamespaceIfExists(String term) {
        return Optional.ofNullable(namespaces.get(term));
    }

    public static CompoundExportMetadataField getCompoundField(ExportMetadataNamespace namespace, String name, List<ExportMetadataField> fields) {
        return getCompoundFields(namespace, name, List.of(fields));
    }

    public static CompoundExportMetadataField getCompoundFields(ExportMetadataNamespace namespace, String name, List<List<ExportMetadataField>> fields) {
        var field = new CompoundExportMetadataField();
        field.setName(name);
        field.setNamespace(namespace);
        field.setValues(fields);

        return field;
    }

    public static ExportMetadataField getMetadataFieldJson(ExportMetadataNamespace namespace, String name, List<JsonObject> value) {
        var field = new RawJsonExportMetadataField();
        field.setName(name);
        field.setNamespace(namespace);
        field.setValues(value);

        return field;
    }

    public static ExportMetadataField getMetadataField(ExportMetadataNamespace namespace, String name, String value) {
        return getMetadataField(namespace, name, List.of(value));
    }

    public static ExportMetadataField getMetadataField(ExportMetadataNamespace namespace, String name, List<String> values) {
        var field = new PrimitiveExportMetadataField();
        field.setName(name);
        field.setNamespace(namespace);
        field.setValues(values);

        return field;
    }
}
