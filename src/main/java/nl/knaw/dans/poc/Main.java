package nl.knaw.dans.poc;

import jakarta.xml.bind.annotation.adapters.HexBinaryAdapter;
import nl.knaw.dans.poc.export.CompoundExportMetadataField;
import nl.knaw.dans.poc.export.ExportMetadataField;
import nl.knaw.dans.poc.export.MetadataFieldFactory;
import nl.knaw.dans.poc.export.PrimitiveExportMetadataField;
import nl.knaw.dans.poc.export.RawJsonExportMetadataField;
import nl.knaw.dans.poc.ddingest.service.mapper.DepositToDvDatasetMetadataMapperFactory;
import nl.knaw.dans.lib.dataverse.model.dataset.CompoundField;
import nl.knaw.dans.lib.dataverse.model.dataset.ControlledMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.ControlledSingleValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.Dataset;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveMultiValueField;
import nl.knaw.dans.lib.dataverse.model.dataset.PrimitiveSingleValueField;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import nl.knaw.dans.poc.bagit.BagGenerator;
import nl.knaw.dans.poc.bagit.DatasetFieldConstant;
import nl.knaw.dans.poc.bagit.JsonLDTerm;
import nl.knaw.dans.poc.depositmanager.Deposit;
import nl.knaw.dans.poc.depositmanager.DepositManagerImpl;
import nl.knaw.dans.poc.depositmanager.XPathEvaluator;
import nl.knaw.dans.poc.depositmanager.XmlReaderImpl;
import org.w3c.dom.Document;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class Main {

    public static Map<String, Path> fileMapping = new HashMap<>();

    public static Path getInputPath(String[] args) {
        if (args.length == 0 || args[0] == null) {
            var s = Main.class.getResource("/input/6a6632f1-91d2-49ba-8449-a8d2b539267a");
            assert s != null;
            return Path.of(s.getPath());
        }

        return Path.of(args[0]);
    }
    public static void main(String[] args) throws IOException {
        var path = getInputPath(args);
        System.out.println("Reading from path: " + path);

        var xmlReader = new XmlReaderImpl();
        var depositManager = new DepositManagerImpl(xmlReader);

        // this will only work for exactly 2 terms
        var vocabsResolver = new VocabsResolver();

        // This should be some kind of configuration
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
            // load the deposit using the dd-sword2 DepositManager
            var deposit = depositManager.loadDeposit(path);
            var globalId = deposit.getDoi();

            // Get the dataset metadata using the dd-ingest-flow metadata mapper
            var dataset = new DepositToDvDatasetMetadataMapperFactory(Map.of(), Map.of())
                .createMapper(true)
                .toDataverseDataset(deposit.getDdm(), null, deposit.getAgreements(), null, null, deposit.getVaultMetadata());

            // get the .xml file
            var dataciteXml = DatasetToDataciteConverter.convertDatasetToDataciteXml(globalId, deposit.getDdmPath());
            System.out.println("DATACITE XML OUTPUT: " + dataciteXml);

            // build the json ld map
            var documentId = UUID.randomUUID().toString();
            var json = convertDatasetToMetadataFields(dataset, vocabsResolver, deposit.getDdm(), deposit.getFilesXml(), deposit);

            // some OAI ORE specific details
            var document = Json.createObjectBuilder();
            document.add("@id", documentId);
            document.add("@type", "ore:ResourceMap");
            document.add("dcterms:modified", LocalDate.now().toString());
            document.add("dcterms:creator", "This PoC");
            document.add("ore:describes", json);

            // add the namespaces to the @context root object
            var context = Json.createObjectBuilder(MetadataFieldFactory.getNamespaceMap());
            document.add("@context", context);

            // this is the final document
            var output = document.build();

            var writer = new StringWriter();
            var map = new HashMap<String, Object>();
            map.put(JsonGenerator.PRETTY_PRINTING, true);
            JsonWriterFactory writerFactory = Json.createWriterFactory(map);
            JsonWriter jsonWriter = writerFactory.createWriter(writer);
            jsonWriter.writeObject(output);
            jsonWriter.close();
            System.out.println("JSON-LD OUTPUT: " + writer.toString());

            // write to /tmp/bag.zip
            var outputStream = new FileOutputStream("/tmp/bag.zip");
            var newBag = new BagGenerator(output, dataciteXml, fileMapping)
                .generateBag(outputStream);

            // Note: the LocalSubmitToArchiveCommand renames the files, something that does not happen here
            System.out.println("Output bag created: /tmp/bag.zip");
        }
        catch (Throwable t) {
            t.printStackTrace();
        }

    }

    public static String calculateChecksum(Path path) throws IOException, NoSuchAlgorithmException {
        var is = new DigestInputStream(new FileInputStream(path.toFile()), MessageDigest.getInstance("SHA-1"));
        var output = is.readAllBytes();

        var bytes = is.getMessageDigest().digest();
        return new HexBinaryAdapter().marshal(bytes).toLowerCase();
    }

    private static JsonObjectBuilder convertDatasetToMetadataFields(Dataset dataset, VocabsResolver vocabsResolver, Document datasetXml, Document filesXml, Deposit deposit) {
        var version = dataset.getDatasetVersion();
        var blocks = version.getMetadataBlocks();
        var results = new ArrayList<ExportMetadataField>();

        // Map each of the dans-dataverse-lib metadata fields to a ExportMetadataField object
        // This is in an attempt to create some kind of common library interface that works with both dataverse
        // and whatever we are going to make.
        // The result has a bunch of fields with a namespace (or null) and the value, which might be a compound field or a simple primitive field.
        for (var block : blocks.entrySet()) {
            var blockNamespace = MetadataFieldFactory.getNamespaceIfExists(block.getKey())
                .orElse(null);

            for (var field : block.getValue().getFields()) {
                var fieldNamespace = MetadataFieldFactory.getNamespaceIfExists(field.getTypeName())
                    .orElse(blockNamespace);

                if (field instanceof PrimitiveSingleValueField) {
                    var result = MetadataFieldFactory.getMetadataField(fieldNamespace, field.getTypeName(), ((PrimitiveSingleValueField) field).getValue());
                    results.add(result);
                }
                else if (field instanceof PrimitiveMultiValueField) {
                    var values = ((PrimitiveMultiValueField) field).getValue();

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

        // the list of ExportMetadataField's is converted to the ORE json map here
        var metadataMap = buildJsonMap(results);

        var description = XPathEvaluator.strings(datasetXml, "//dc:description")
            .map(StringEscapeUtils::escapeXml10)
            .collect(Collectors.joining("; "));

        var title = XPathEvaluator.strings(datasetXml, "//dc:title")
            .map(StringEscapeUtils::escapeXml10)
            .filter(t -> StringUtils.isNotBlank(t) && !"N/A".equals(t))
            .collect(Collectors.joining("; "));

        // this is logic from Dataverse
        if (StringUtils.isBlank(title)) {
            title = ":unav";
        }

        var fileList = Json.createArrayBuilder();
        var fileIdList = Json.createArrayBuilder();
        XPathEvaluator.nodes(filesXml, "//files:file")
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

                    var checksum = calculateChecksum(deposit.getBagDir().resolve(path));
                    builder.add("dvcore:checksum", Json.createObjectBuilder()
                        .add("@type", "SHA-1")
                        .add("@value", checksum)
                    );
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
                catch (NoSuchAlgorithmException e) {
                    throw new RuntimeException(e);
                }
                builder.add("@id", fileId);
                builder.add("@type", "ore:AggregatedResource");

                fileList.add(builder.build());
                fileIdList.add(fileId);

                // this is for the bag generator, so it knows about the files
                fileMapping.put(fileId, deposit.getBagDir().resolve(filepath));
                fileMapping.put(filepath, deposit.getBagDir().resolve(filepath));
            });

        var fileListBuilt = fileList.build();
        metadataMap.add("@id", deposit.getDoi())
            .add(DatasetFieldConstant.description, description)
            .add("@type",
                Json.createArrayBuilder().add(JsonLDTerm.ore("Aggregation").getLabel())
                    .add(JsonLDTerm.schemaOrg("Dataset").getLabel()))
            .add(JsonLDTerm.schemaOrg("version").getLabel(), "1.0")
            .add(JsonLDTerm.ore("aggregates").getLabel(), fileListBuilt)
            .add("schema:hasPart", fileIdList)
            .add(JsonLDTerm.schemaOrg("name").getLabel(), title);

        if (version.getLastUpdateTime() != null) {
            metadataMap.add(JsonLDTerm.schemaOrg("dateModified").getLabel(), version.getLastUpdateTime());
        }

        if (version.getTermsOfAccess() != null) {
            metadataMap.add(JsonLDTerm.schemaOrg("license").getLabel(),
                version.getTermsOfAccess());
        }

        return metadataMap;
    }

    // converts the list of metadata fields to the Json object
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