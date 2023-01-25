/*
 * Copyright (C) 2022 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.ingest.core.service.mapper.mapping;

import lombok.extern.slf4j.Slf4j;
import org.example.service.XPathEvaluator;
import org.w3c.dom.Node;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class AccessRights extends Base {
    private static final Map<String, Boolean> accessRightsToDefaultRestrict = Map.of(
        "OPEN_ACCESS", false,
        "OPEN_ACCESS_FOR_REGISTERED_USERS", true,
        "REQUEST_PERMISSION", true,
        "NO_ACCESS", true
    );

    public static boolean toDefaultRestrict(Node node) {
        var text = node.getTextContent().trim();
        return accessRightsToDefaultRestrict.getOrDefault(text, true);
    }

    public static boolean isEnableRequests(Node accessRightsNode, Node filesNode) {
        var accessibleToRights = XPathEvaluator.strings(filesNode, "//files:file/ddm:accessibleToRights")
            .collect(Collectors.toList());

        var numberOfFiles = XPathEvaluator.strings(filesNode, "//files:file").count();

        var isImplicit = numberOfFiles > accessibleToRights.size() && "NO_ACCESS".equals(accessRightsNode.getTextContent().trim());
        var isExplicit = accessibleToRights.stream()
            .anyMatch("NONE"::equals);

        return !isImplicit && !isExplicit;
    }
}
