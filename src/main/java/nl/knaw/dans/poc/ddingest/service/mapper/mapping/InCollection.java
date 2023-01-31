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
package nl.knaw.dans.poc.ddingest.service.mapper.mapping;

import lombok.extern.slf4j.Slf4j;
import org.w3c.dom.Node;

import java.util.Optional;

import static nl.knaw.dans.poc.depositmanager.XmlNamespaces.NAMESPACE_DDM;

@Slf4j
public final class InCollection extends Base {

    public static String toCollection(Node node) {
        return Optional.ofNullable(node.getAttributes())
            .map(n -> Optional.ofNullable(n.getNamedItemNS(NAMESPACE_DDM, "valueURI")))
            .flatMap(i -> i)
            .map(Node::getTextContent)
            .orElseGet(() -> {
                log.error("Missing valueURI attribute on ddm:inCollection node");
                return null;
            });
    }
}
