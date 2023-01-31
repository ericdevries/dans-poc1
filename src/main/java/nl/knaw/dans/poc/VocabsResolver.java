package nl.knaw.dans.poc;

import lombok.extern.slf4j.Slf4j;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class VocabsResolver {

    private final Map<String, JsonObject> vocabTerms = new HashMap<>();
    private final Map<String, JsonObject> vocabMocks = new HashMap<>();

    public VocabsResolver() throws IOException {
        // load config from json
        try (var input = getClass().getResourceAsStream("/vocabs/config.json")) {
            var reader = Json.createReader(input);
            var results = reader.readArray();

            for (var element : results) {
                var e = ((JsonObject) element);
                var name = e.getString("term-uri-field");
                vocabTerms.put(name, e);
            }
        }

        // load specific configs to mimic vocabs server
        loadConfig("https://www.narcis.nl/classification/D20000", "/vocabs/dansAudience_D20000.json");
        loadConfig("https://www.narcis.nl/classification/D24000", "/vocabs/dansAudience_D24000.json");
    }

    void loadConfig(String url, String name) throws IOException {
        try (var input = getClass().getResourceAsStream(name)) {
            var reader = Json.createReader(input);
            var results = reader.readObject();
            vocabMocks.put(url, results);
        }
    }

    public boolean isTerm(String term) {
        return vocabTerms.containsKey(term);
    }

    public JsonObject convertTerm(String term, String value) {
        var config = vocabTerms.get(term);
        var values = vocabMocks.get(value);

        return filterResponse(config, values, value);
    }

    // from DatasetFieldServiceBean, line 519
    private JsonObject filterResponse(JsonObject cvocEntry, JsonObject readObject, String termUri) {

        JsonObjectBuilder job = Json.createObjectBuilder();
        JsonObject filtering = cvocEntry.getJsonObject("retrieval-filtering");
        log.debug("RF: " + filtering.toString());
        JsonObject managedFields = cvocEntry.getJsonObject("managed-fields");
        log.debug("MF: " + managedFields.toString());
        for (String filterKey : filtering.keySet()) {
            if (!filterKey.equals("@context")) {
                try {
                    JsonObject filter = filtering.getJsonObject(filterKey);
                    log.debug("F: " + filter.toString());
                    JsonArray params = filter.getJsonArray("params");
                    if (params == null) {
                        params = Json.createArrayBuilder().build();
                    }
                    log.debug("Params: " + params.toString());
                    List<Object> vals = new ArrayList<Object>();
                    for (int i = 0; i < params.size(); i++) {
                        String param = params.getString(i);
                        if (param.startsWith("/")) {
                            // Remove leading /
                            param = param.substring(1);
                            String[] pathParts = param.split("/");
                            log.debug("PP: " + String.join(", ", pathParts));
                            JsonValue curPath = readObject;
                            for (int j = 0; j < pathParts.length - 1; j++) {
                                if (pathParts[j].contains("=")) {
                                    JsonArray arr = ((JsonArray) curPath);
                                    for (int k = 0; k < arr.size(); k++) {
                                        String[] keyVal = pathParts[j].split("=");
                                        log.debug("Looking for object where " + keyVal[0] + " is " + keyVal[1]);
                                        JsonObject jo = arr.getJsonObject(k);
                                        String val = jo.getString(keyVal[0]);
                                        String expected = keyVal[1];
                                        if (expected.equals("@id")) {
                                            expected = termUri;
                                        }
                                        if (val.equals(expected)) {
                                            log.debug("Found: " + jo.toString());
                                            curPath = jo;
                                            break;
                                        }
                                    }
                                }
                                else {
                                    curPath = ((JsonObject) curPath).get(pathParts[j]);
                                    log.debug("Found next Path object " + curPath.toString());
                                }
                            }
                            JsonValue jv = ((JsonObject) curPath).get(pathParts[pathParts.length - 1]);
                            if (jv.getValueType().equals(JsonValue.ValueType.STRING)) {
                                vals.add(i, ((JsonString) jv).getString());
                            }
                            else if (jv.getValueType().equals(JsonValue.ValueType.ARRAY)) {
                                vals.add(i, jv);
                            }
                            else if (jv.getValueType().equals(JsonValue.ValueType.OBJECT)) {
                                vals.add(i, jv);
                            }
                            log.debug("Added param value: " + i + ": " + vals.get(i));
                        }
                        else {
                            log.debug("Param is: " + param);
                            // param is not a path - either a reference to the term URI
                            if (param.equals("@id")) {
                                log.debug("Adding id param: " + termUri);
                                vals.add(i, termUri);
                            }
                            else {
                                // or a hardcoded value
                                log.debug("Adding hardcoded param: " + param);
                                vals.add(i, param);
                            }
                        }
                    }
                    // Shortcut: nominally using a pattern of {0} and a param that is @id or
                    // hardcoded value allows the same options as letting the pattern itself be @id
                    // or a hardcoded value
                    String pattern = filter.getString("pattern");
                    log.debug("Pattern: " + pattern);
                    if (pattern.equals("@id")) {
                        log.debug("Added #id pattern: " + filterKey + ": " + termUri);
                        job.add(filterKey, termUri);
                    }
                    else if (pattern.contains("{")) {
                        if (pattern.equals("{0}")) {
                            if (vals.get(0) instanceof JsonArray) {
                                job.add(filterKey, (JsonArray) vals.get(0));
                            }
                            else {
                                job.add(filterKey, (String) vals.get(0));
                            }
                        }
                        else {
                            String result = MessageFormat.format(pattern, vals.toArray());
                            log.debug("Result: " + result);
                            job.add(filterKey, result);
                            log.debug("Added : " + filterKey + ": " + result);
                        }
                    }
                    else {
                        log.debug("Added hardcoded pattern: " + filterKey + ": " + pattern);
                        job.add(filterKey, pattern);
                    }
                }
                catch (Exception e) {
                    log.warn("External Vocabulary: " + termUri + " - Failed to find value for " + filterKey + ": "
                        + e.getMessage());
                }
            }
        }
        JsonObject filteredResponse = job.build();
        if (filteredResponse.isEmpty()) {
            log.error("Unable to filter response for term: " + termUri + ",  received: " + readObject.toString());
            //Better to store nothing in this case so unknown values don't propagate to exported metadata (we'll just send the termUri itself in those cases).
            return null;
        }
        else {
            return filteredResponse;
        }
    }
}
