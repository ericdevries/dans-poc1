package org.example;

import nl.knaw.dans.export.CompoundExportMetadataField;
import nl.knaw.dans.export.ExportMetadataField;
import nl.knaw.dans.export.MetadataFieldFactory;
import nl.knaw.dans.export.PrimitiveExportMetadataField;
import nl.knaw.dans.export.RawJsonExportMetadataField;
import nl.knaw.dans.ingest.core.service.mapper.DepositToDvDatasetMetadataMapperFactory;
import nl.knaw.dans.lib.dataverse.model.dataset.CompoundField;
import nl.knaw.dans.lib.dataverse.model.dataset.ControlledMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.ControlledSingleValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import org.example.service.DepositManagerImpl;
import org.example.service.XmlReaderImpl;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        var s = Main.class.getResource("/input/6a6632f1-91d2-49ba-8449-a8d2b539267a");
        var path = Path.of(s.getPath());

        var xmlReader = new XmlReaderImpl();
        var depositManager = new DepositManagerImpl(xmlReader);

        var vocabsResolver = new VocabsResolver();

        var authorNamespace = MetadataFieldFactory.getNamespace("author", "http://purl.org/dc/terms/creator");
        var authorIdentifierNamespace = MetadataFieldFactory.getNamespace("authorIdentifier", "http://purl.org/spar/datacite/AgentIdentifier");
        var authorIdentifierSchemeNamespace = MetadataFieldFactory.getNamespace("authorIdentifierScheme", "http://purl.org/spar/datacite/AgentIdentifierScheme");
        var citationNamespace = MetadataFieldFactory.getNamespace("citation", "https://dataverse.org/schema/citation/");
        var dansDataVaultMetadataNamespace = MetadataFieldFactory.getNamespace("dansDataVaultMetadata", "https://dar.dans.knaw.nl/schema/dansDataVaultMetadata#");
        var dansRelationMetadataNamespace = MetadataFieldFactory.getNamespace("dansRelationMetadata", "https://dar.dans.knaw.nl/schema/dansRelationMetadata#");
        var dansRightsNamespace = MetadataFieldFactory.getNamespace("dansRights", "https://dar.dans.knaw.nl/schema/dansRights#");
        var dctermsNamespace = MetadataFieldFactory.getNamespace("dcterms", "http://purl.org/dc/terms/");
        var dvcoreNamespace = MetadataFieldFactory.getNamespace("dvcore", "https://dataverse.org/schema/core#");
        var langNamespace = MetadataFieldFactory.getNamespace("lang", "@language");
        var oreNamespace = MetadataFieldFactory.getNamespace("ore", "http://www.openarchives.org/ore/terms/");
        var schemaNamespace = MetadataFieldFactory.getNamespace("schema", "http://schema.org/");
        var subjectNamespace = MetadataFieldFactory.getNamespace("subject", "http://purl.org/dc/terms/subject");
        var termNameNamespace = MetadataFieldFactory.getNamespace("termName", "https://schema.org/name");
        var titleNamespace = MetadataFieldFactory.getNamespace("title", "http://purl.org/dc/terms/title");
        var valueNamespace = MetadataFieldFactory.getNamespace("value", "@value");
        var vocabularyNameNamespace = MetadataFieldFactory.getNamespace("vocabularyName", "https://dataverse.org/schema/vocabularyName");
        var vocabularyUriNamespace = MetadataFieldFactory.getNamespace("vocabularyUri", "https://dataverse.org/schema/vocabularyUri");

        try {
            var deposit = depositManager.loadDeposit(path);
            var globalId = deposit.getDoi();

            var dataset = new DepositToDvDatasetMetadataMapperFactory(Map.of(), Map.of())
                .createMapper(true)
                .toDataverseDataset(deposit.getDdm(), null, deposit.getAgreements(), null, null, deposit.getVaultMetadata());

            var result = DatasetToDataciteConverter.convertDatasetToDataciteXml(globalId, deposit.getDdmPath());

            //            System.out.println("RESULT: " + result);
            //
            //            var fields = List.of(
            //                MetadataFieldFactory.getMetadataField(citationNamespace, "authorName", "D.N. van den Aarden"),
            //                MetadataFieldFactory.getMetadataField(citationNamespace, "authorAffiliation", "Utrecht University"),
            //                MetadataFieldFactory.getMetadataField(null, "authorIdentifierScheme", "DAI"),
            //                MetadataFieldFactory.getMetadataField(null, "authorIdentifier", "123456789")
            //            );
            //
            //            var author = MetadataFieldFactory.getCompoundField(authorNamespace, "author", fields);
            //
            //            var contact = MetadataFieldFactory.getCompoundField(citationNamespace, "datasetContact", List.of(
            //                MetadataFieldFactory.getMetadataField(citationNamespace, "datasetContactName", "user001"),
            //                MetadataFieldFactory.getMetadataField(citationNamespace, "datasetContactAffiliation", "DANS"),
            //                MetadataFieldFactory.getMetadataField(citationNamespace, "datasetContactEmail", "user001@dans.knaw.nl")
            //            ));
            //
            //            var personalDataPresent = MetadataFieldFactory.getMetadataField(dansRightsNamespace,
            //                "dansPersonalDataPresent", "Unknown");
            //            var dansNbn = MetadataFieldFactory.getMetadataField(dansDataVaultMetadataNamespace,
            //                "dansNbn", "urn:nbn:nl:ui:13-ar2-u8v");
            //            var distDate = MetadataFieldFactory.getMetadataField(citationNamespace,
            //                "distributionDate", "2013-05-01");
            //
            //            var allFields = new ArrayList<ExportMetadataField>();
            //            allFields.add(author);
            //            allFields.add(contact);
            //            allFields.add(personalDataPresent);
            //            allFields.add(dansNbn);
            //            allFields.add(distDate);
            //
            //            var json = buildJsonMap(allFields);
            var json = convertDatasetToMetadataFields(dataset, vocabsResolver);
            var output = json.build();

            var writer = new StringWriter();
            var map = new HashMap<String, Object>();
            map.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriterFactory writerFactory = Json.createWriterFactory(map);
            JsonWriter jsonWriter = writerFactory.createWriter(writer);
            jsonWriter.writeObject(output);
            jsonWriter.close();
            System.out.println("OUTPUT: " + writer.toString());

            //            "author": {
            //                "citation:authorName": "D.N. van den Aarden",
            //                    "citation:authorAffiliation": "Utrecht University",
            //                    "authorIdentifierScheme": "DAI",
            //                    "authorIdentifier": "123456789"
            //            },
            //            "citation:datasetContact": {
            //                "citation:datasetContactName": "Test01  User01",
            //                    "citation:datasetContactAffiliation": "DANS",
            //                    "citation:datasetContactEmail": "user001@dans.knaw.nl"
            //            },
            //            "citation:dsDescription": {
            //                "citation:dsDescriptionValue": "<p>Lorem ipsum dolor sit amet, consectetur adipiscing elit.</p>"
            //            },
            //            "citation:otherId": {
            //                "citation:otherIdValue": "doi:"
            //            },
            //            "dansRights:dansPersonalDataPresent": "Unknown",
            //                "dansDataVaultMetadata:dansNbn": "urn:nbn:nl:ui:13-ar2-u8v",
            //                "citation:distributionDate": "2013-05-01",
            //                "subject": "Medicine, Health and Life Sciences",
            //                "dansDataVaultMetadata:dansSwordToken": "sword:123e4567-e89b-12d3-a456-556642440000",
            //                "dansRights:dansMetadataLanguage": [
            //            "English",
            //                "Latin"
            //    ],

            //            var version = dataset.getDatasetVersion();
            //            version.setId(1);
            //            version.setIdentifier(deposit.getId());
            //            version.setDatasetPersistentId(globalId);
            //
            //            var dbg = new ObjectMapper()
            //                .writerWithDefaultPrettyPrinter()
            //                .writeValueAsString(dataset);
            //            System.out.println("TEST: " + dbg);
            //
            ////            var fileList = Json.createArrayBuilder();
            ////            XPathEvaluator.nodes(deposit.getFilesXml(), "//files:file")
            ////                .forEach(f -> {
            ////                    var filepath = f.getAttributes().getNamedItem("filepath").getTextContent();
            ////                    var type = XPathEvaluator.strings(f, "//dcterms:format").findFirst().orElse("");
            ////                    var description = XPathEvaluator.strings(f, "//dcterms:description").findFirst().orElse("");
            ////
            ////                    var builder = Json.createObjectBuilder();
            ////                    builder.add("schema:description", description);
            ////                    builder.add("schema:name", Path.of(filepath).getFileName().toString());
            ////                    builder.add("@id", "file:///" + UUID.randomUUID());
            ////                    builder.add("@type", "ore:AggregatedResource");
            ////                    builder.add("schema:fileFormat", type);
            ////
            ////                    fileList.add(builder.build());
            ////                });
            ////
            ////            var fileListBuilt = fileList.build();
            //           /*
            //        "schema:description": "Another empty file",
            //        "schema:name": "vacio.txt",
            //        "dvcore:restricted": false,
            //        "dvcore:directoryLabel": "sub/sub",
            //        "schema:version": 1,
            //        "dvcore:datasetVersionId": 11,
            //        "@id": "https://dar.dans.knaw.nl/file.xhtml?fileId=56",
            //        "schema:sameAs": "https://dar.dans.knaw.nl/api/access/datafile/56",
            //        "@type": "ore:AggregatedResource",
            //        "schema:fileFormat": "text/plain",
            //        "dvcore:filesize": 0,
            //        "dvcore:storageIdentifier": "file://1859fce718a-fcb5a2913afc",
            //        "dvcore:rootDataFileId": -1,
            //        "dvcore:checksum": {
            //          "@type": "SHA-1",
            //          "@value": "da39a3ee5e6b4b0d3255bfef95601890afd80709"
            //        }
            //            */
            //            var oreMapBuilder = new OREMapBuilder();
            //            var oreMap = oreMapBuilder.getOREMapBuilder(deposit.getDdmPath(), dataset, deposit)
            //                .build();
            //
            //            System.out.println("JSON: " + oreMap.toString());
            //            var outputStream = new FileOutputStream("/tmp/bag.zip.partial");
            //            var newBag = new BagGenerator(oreMap, result, oreMapBuilder.fileMapping)
            //                .generateBag(outputStream);
            //
            //            System.out.println("DONE");
            //
        }
        catch (Throwable t) {
            t.printStackTrace();
        }

    }

    private static JsonObjectBuilder convertDatasetToMetadataFields(Dataset dataset, VocabsResolver vocabsResolver) {
        var version = dataset.getDatasetVersion();
        var blocks = version.getMetadataBlocks();

        var results = new ArrayList<ExportMetadataField>();

        for (var block : blocks.entrySet()) {
            var blockNamespace = MetadataFieldFactory.getNamespaceIfExists(block.getKey())
                .orElse(null);

            for (var field : block.getValue().getFields()) {
                System.out.println("FIELD: " + field.getTypeName() + " - " + field.getTypeClass() + " - " + field.getClass());
                var fieldNamespace = MetadataFieldFactory.getNamespaceIfExists(field.getTypeName())
                    .orElse(blockNamespace);

                if (field instanceof PrimitiveSingleValueField) {
                    var result = MetadataFieldFactory.getMetadataField(fieldNamespace, field.getTypeName(), ((PrimitiveSingleValueField) field).getValue());
                    results.add(result);
                }
                else if (field instanceof PrimitiveMultiValueField) {
                    var values = ((PrimitiveMultiValueField) field).getValue();

                    System.out.println("IS TERM: " + vocabsResolver.isTerm(field.getTypeName()));
                    if (vocabsResolver.isTerm(field.getTypeName())) {
                        var newValues = values.stream().map(t -> vocabsResolver.convertTerm(field.getTypeName(), t))
                            .collect(Collectors.toList());
                        var result = MetadataFieldFactory.getMetadataFieldJson(fieldNamespace, field.getTypeName(), newValues);
                        results.add(result);
                    }
                    else {
                        var result = MetadataFieldFactory.getMetadataField(fieldNamespace, field.getTypeName(), ((PrimitiveMultiValueField) field).getValue());
                        results.add(result);
                    }
                }

                else if (field instanceof CompoundField) {
                    var values = ((CompoundField) field).getValue();
                    var metadataFields = values.stream()
                        .map(record -> record.entrySet().stream()
                            .map(entry -> {
                                var ns = MetadataFieldFactory.getNamespaceIfExists(entry.getKey()).orElse(fieldNamespace);
                                return MetadataFieldFactory.getMetadataField(ns, entry.getKey(), entry.getValue().getValue());
                            })
                            .collect(Collectors.toList()))
                        .collect(Collectors.toList());

                    var result = MetadataFieldFactory.getCompoundFields(fieldNamespace, field.getTypeName(), metadataFields);
                    results.add(result);
                }
                else if (field instanceof ControlledMultiValueField) {
                    var result = MetadataFieldFactory.getMetadataField(fieldNamespace, field.getTypeName(), ((ControlledMultiValueField) field).getValue());
                    results.add(result);
                }
                else if (field instanceof ControlledSingleValueField) {
                    var result = MetadataFieldFactory.getMetadataField(fieldNamespace, field.getTypeName(), ((ControlledSingleValueField) field).getValue());
                    results.add(result);
                }
            }
        }

        return buildJsonMap(results);
    }

    public static JsonObjectBuilder buildJsonMap(List<ExportMetadataField> exportMetadataFields) {
        var builder = Json.createObjectBuilder();

        for (var field : exportMetadataFields) {
            var name = field.getName();

            if (field.getNamespace() != null) {
                if (!field.getNamespace().getTerm().equals(name)) {
                    name = field.getNamespace().getTerm() + ":" + name;
                }
            }

            if (field instanceof CompoundExportMetadataField) {
                var compoundField = (CompoundExportMetadataField) field;
                var children = compoundField.getValues().stream().map(Main::buildJsonMap).collect(Collectors.toList());

                if (children.size() != 1) {
                    var arrayBuilder = Json.createArrayBuilder();

                    for (var value : children) {
                        arrayBuilder.add(value);
                    }

                    builder.add(name, arrayBuilder);
                }
                else {
                    builder.add(name, children.get(0));
                }
            }
            else if (field instanceof RawJsonExportMetadataField) {
                var primitiveField = (RawJsonExportMetadataField) field;

                if (primitiveField.getValues().size() != 1) {
                    var arrayBuilder = Json.createArrayBuilder();

                    for (var value : primitiveField.getValues()) {
                        arrayBuilder.add(value);
                    }

                    builder.add(name, arrayBuilder);
                }
                else {
                    builder.add(name, primitiveField.getValues().get(0));
                }
            }
            else {
                var primitiveField = (PrimitiveExportMetadataField) field;

                if (primitiveField.getValues().size() != 1) {
                    var arrayBuilder = Json.createArrayBuilder();

                    for (var value : primitiveField.getValues()) {
                        arrayBuilder.add(value);
                    }

                    builder.add(name, arrayBuilder);
                }
                else {
                    builder.add(name, primitiveField.getValues().get(0));
                }
            }
        }

        return builder;
    }
}