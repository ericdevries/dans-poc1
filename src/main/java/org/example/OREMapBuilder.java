package org.example;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import nl.knaw.dans.lib.dataverse.model.dataset.CompoundField;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.MetadataField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.example.dataverse.DatasetAuthor;
import org.example.dataverse.bagit.DatasetFieldConstant;
import org.example.dataverse.bagit.JsonLDNamespace;
import org.example.dataverse.bagit.JsonLDTerm;
import org.example.service.Deposit;
import org.example.service.XPathEvaluator;
import org.example.service.XmlReaderImpl;
import org.xml.sax.SAXException;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

public class OREMapBuilder {

    public Map<String, Path> fileMapping = new HashMap<>();

    Map<String, FieldDescription> fieldmap = new HashMap<>();

    void processField(MetadataField field) {
        if (field.getTypeClass().equals("compound")) {

        }
        else {

        }
    }

    String getName(String name, JsonLDNamespace namespace) {
        var definitiveName = name;
        var prefix = namespace.getPrefix();

        if (!prefix.equals(name)) {
            definitiveName = prefix + ":" + name;
        }

        return definitiveName;
    }

    public JsonObjectBuilder getOREMapBuilder(Path datasetXml, Dataset dataset, Deposit deposit) throws ParserConfigurationException, IOException, SAXException {
        // build metadata map
        var citationField = new FieldDescription("citation", "https://dataverse.org/schema/citation/");
        fieldmap.put("citation", citationField);
        fieldmap.put("author", new FieldDescription("author", "http://purl.org/dc/terms/creator"));
        fieldmap.put("authorName", citationField);
        fieldmap.put("authorAffiliation", citationField);
        fieldmap.put("authorIdentifierScheme", new FieldDescription("author", null));
        fieldmap.put("authorIdentifier", new FieldDescription("author", null));

        //        "author": {
        //            "citation:authorName": "D.N. van den Aarden",
        //                "citation:authorAffiliation": "Utrecht University",
        //                "authorIdentifierScheme": "DAI",
        //                "authorIdentifier": "123456789"
        //        },

        var xmlReader = new XmlReaderImpl();
        var doc = xmlReader.readXmlFile(datasetXml);

        var authors = XPathEvaluator.nodes(doc, "//dcx-dai:creatorDetails/dcx-dai:author")
            .map(DatasetAuthor::parseAuthor)
            .collect(Collectors.toList());

        var description = XPathEvaluator.strings(doc, "//dc:description")
            .map(StringEscapeUtils::escapeXml10)
            .collect(Collectors.joining("; "));

        var title = XPathEvaluator.strings(doc, "//dc:title")
            .map(StringEscapeUtils::escapeXml10)
            .filter(t -> StringUtils.isNotBlank(t) && !"N/A".equals(t))
            .collect(Collectors.joining("; "));

        if (StringUtils.isBlank(title)) {
            title = ":unav";
        }

        // treemap has sorted keys
        var localContext = new TreeMap<String, String>();

        localContext.putIfAbsent(JsonLDNamespace.ore.getPrefix(), JsonLDNamespace.ore.getUrl());
        localContext.putIfAbsent(JsonLDNamespace.dcterms.getPrefix(), JsonLDNamespace.dcterms.getUrl());
        localContext.putIfAbsent(JsonLDNamespace.dvcore.getPrefix(), JsonLDNamespace.dvcore.getUrl());
        localContext.putIfAbsent(JsonLDNamespace.schema.getPrefix(), JsonLDNamespace.schema.getUrl());

        var aggBuilder = Json.createObjectBuilder();
        var version = dataset.getDatasetVersion();
        var id = version.getDatasetPersistentId();

        // parse metadata
        for (var entry : version.getMetadataBlocks().entrySet()) {
            var key = entry.getKey();

            if (fieldmap.containsKey(key)) {
                var field = fieldmap.get(key);
                // get namespace
                // if it is defined in the fieldmap, use that
                // otherwise generate a new one based on the name of the field (in this case the metablock name)
                var namespace = getNamespace(key, field);

                localContext.putIfAbsent(field.getName(), namespace.getUrl());

                for (var metadataField : entry.getValue().getFields()) {
                    var typeName = metadataField.getTypeName();
                    var field2 = fieldmap.get(typeName);

                    if (field2 == null) {
                        continue;
                    }

                    var namespace2 = getNamespace(typeName, field2);
                    localContext.putIfAbsent(field2.getName(), namespace2.getUrl());

                    // compound has multiple values in it
                    if (metadataField.getTypeClass().equals("compound")) {
                        var values = ((CompoundField) metadataField).getValue();

                        var items = values.stream().map(item -> {
                            var itemBuilder = Json.createObjectBuilder();

                            for (var e : item.entrySet()) {
                                var field3 = fieldmap.get(e.getKey());

                                if (field3 == null) {
                                    continue;
                                }
                                var namespace3 = getNamespace(e.getKey(), fieldmap.get(e.getKey()));
                                localContext.putIfAbsent(e.getKey(), namespace3.getUrl());
                                var name = getName(e.getKey(), namespace3);

                                itemBuilder.add(name, e.getValue().getValue());
                            }

                            return itemBuilder;
                        }).collect(Collectors.toList());

                        if (items.size() == 1) {
                            aggBuilder.add(getName(typeName, namespace2), items.get(0));
                        }
                        else {
                            var array = Json.createArrayBuilder();

                            for (var i : items) {
                                array.add(i);
                            }
                            aggBuilder.add(getName(typeName, namespace2), array);
                        }
                    }
                    // TODO what about controlledVocabulary
                    else if (metadataField.getTypeClass().equals("primitive")) {
                        var definitiveName = typeName;
                        var prefix = namespace2.getPrefix();
                        if (!prefix.equals(typeName)) {
                            definitiveName = prefix + ":" + typeName;
                        }

                        if (metadataField.isMultiple()) {
                            var array = Json.createArrayBuilder();
                            ((PrimitiveMultiValueField) metadataField).getValue()
                                .stream().forEach(a -> array.add(array));
                            aggBuilder.add(definitiveName, array);
                        }
                        else {
                            aggBuilder.add(definitiveName, ((PrimitiveSingleValueField) metadataField).getValue());
                        }
                    }

                }
            }
        }

        var fileList = Json.createArrayBuilder();
        var fileIdList = Json.createArrayBuilder();
        XPathEvaluator.nodes(deposit.getFilesXml(), "//files:file")
            .forEach(f -> {
                var filepath = f.getAttributes().getNamedItem("filepath").getTextContent();
                var type = XPathEvaluator.strings(f, "//dcterms:format").findFirst().orElse("");
                var desc = XPathEvaluator.strings(f, "//dcterms:description").findFirst().orElse("");
                var path = Path.of(filepath);

                var fileId = "file:///" + UUID.randomUUID();
                var builder = Json.createObjectBuilder();
                builder.add("schema:description", desc);
                builder.add("schema:name", path.getFileName().toString());
                builder.add("schema:sameAs", fileId);
                builder.add("schema:fileFormat", type);
                try {
                    builder.add("dvcore:filesize", Files.size(deposit.getBagDir().resolve(path)));
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                builder.add("@id", fileId);
                builder.add("@type", "ore:AggregatedResource");

                fileList.add(builder.build());
                fileIdList.add(fileId);

                fileMapping.put(fileId, deposit.getBagDir().resolve(filepath));
                fileMapping.put(filepath, deposit.getBagDir().resolve(filepath));
            });

        var fileListBuilt = fileList.build();
        // Add metadata related to the Dataset/DatasetVersion
        aggBuilder.add("@id", id)
            .add(DatasetFieldConstant.description, description)
            .add("@type",
                Json.createArrayBuilder().add(JsonLDTerm.ore("Aggregation").getLabel())
                    .add(JsonLDTerm.schemaOrg("Dataset").getLabel()))
            .add(JsonLDTerm.schemaOrg("version").getLabel(), "1.0")
            // TODO title comes from dataset.xml
            .add(JsonLDTerm.ore("aggregates").getLabel(), fileListBuilt)
            .add("schema:hasPart", fileIdList)
            .add(JsonLDTerm.schemaOrg("name").getLabel(), title);

        if (version.getLastUpdateTime() != null) {
            aggBuilder.add(JsonLDTerm.schemaOrg("dateModified").getLabel(), version.getLastUpdateTime());
        }

        // FIXME publication date does not exist outside of dataverse
        //        addIfNotNull(aggBuilder, JsonLDTerm.schemaOrg("datePublished"), dataset.getPublicationDateFormattedYYYYMMDD());

        if (version.getTermsOfAccess() != null) {
            aggBuilder.add(JsonLDTerm.schemaOrg("license").getLabel(),
                version.getTermsOfAccess());
        }
        JsonObjectBuilder contextBuilder = Json.createObjectBuilder();
        for (Map.Entry<String, String> e : localContext.entrySet()) {
            contextBuilder.add(e.getKey(), e.getValue());
        }

        return Json.createObjectBuilder()
            .add(JsonLDTerm.dcTerms("modified").getLabel(), LocalDate.now().toString())
            .add(JsonLDTerm.dcTerms("creator").getLabel(), "this awesome PoC")
            .add("@type", JsonLDTerm.ore("ResourceMap").getLabel())
            // Define an id for the map itself (separate from the @id of the dataset being
            // described
            .add("@id", "https://dar.dans.knaw.nl/some_id")
            // Add the aggregation (Dataset) itself to the map.
            .add(JsonLDTerm.ore("describes").getLabel(), aggBuilder)
            .add("@context", contextBuilder.build());

        //                aggBuilder.add(JsonLDTerm.ore("aggregates").getLabel(), aggResArrayBuilder.build())
        //                    .add(JsonLDTerm.schemaOrg("hasPart").getLabel(), fileArray.build()).build())
        // and finally add the context
        //            .add("@context", contextBuilder.build());
    }

    private JsonLDNamespace getNamespace(String key, FieldDescription value) {
        System.out.println("key: " + key + " - " + value);
        if (value.getNamespaceUri() != null) {
            return JsonLDNamespace.defineNamespace(value.getName(), value.getNamespaceUri());
        }

        return JsonLDNamespace.defineNamespace(key, "https://dar.dans.knaw.nl/schema/" + key + "#");
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
class FieldDescription {
    String name;
    String namespaceUri;
}
