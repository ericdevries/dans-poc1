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
import org.apache.commons.lang3.StringUtils;
import org.example.service.XPathEvaluator;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.w3c.dom.Node;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.stream.Stream;

import static org.example.service.XmlNamespaces.NAMESPACE_XSI;

@Slf4j
public class Base {
    //    private static final DateFormat dateAvailableFormat = new SimpleDateFormat("yyyy-MM-dd");
    private static final DateFormat dateAvailableFormat = new SimpleDateFormat("yyyy-MM-dd");

    private static final DateTimeFormatter yyyymmddPattern = DateTimeFormat.forPattern("YYYY-MM-dd");

    static boolean hasXsiType(Node node, String xsiType) {
        var attributes = node.getAttributes();

        if (attributes == null) {
            return false;
        }

        return Optional.ofNullable(attributes.getNamedItemNS(NAMESPACE_XSI, "type"))
            .map(item -> {
                var text = item.getTextContent();
                return xsiType.equals(text) || text.endsWith(":" + xsiType);
            })
            .orElse(false);
    }

    public static Optional<Node> getAttribute(Node node, String name) {
        return Optional.ofNullable(node.getAttributes())
            .map(a -> Optional.ofNullable(a.getNamedItem(name)))
            .flatMap(i -> i);
    }

    public static Optional<Node> getAttribute(Node node, String namespaceURI, String name) {
        return Optional.ofNullable(node.getAttributes())
            .map(a -> Optional.ofNullable(a.getNamedItemNS(namespaceURI, name)))
            .flatMap(i -> i);
    }

    public static boolean hasAttribute(Node node, String name) {
        return getAttribute(node, name).isPresent();
    }

    static boolean hasAttribute(Node node, String namespaceURI, String name) {
        return getAttribute(node, namespaceURI, name).isPresent();

    }

    static boolean hasAttributeValue(Node node, String name, String value) {
        return getAttribute(node, name)
            .map(n -> StringUtils.equals(value, n.getTextContent()))
            .orElse(false);

    }

    static boolean hasAttributeValue(Node node, String namespaceURI, String name, String value) {
        return getAttribute(node, namespaceURI, name)
            .map(n -> StringUtils.equals(value, n.getTextContent()))
            .orElse(false);

    }

    public static String asText(Node node) {
        return node.getTextContent();
    }

    public static boolean hasChildNode(Node node, String xpath) {
        return getChildNode(node, xpath).isPresent();
    }

    public static Optional<Node> getChildNode(Node node, String xpath) {
        return XPathEvaluator.nodes(node, xpath).findAny();
    }

    public static Stream<Node> getChildNodes(Node node, String xpath) {
        return XPathEvaluator.nodes(node, xpath);
    }

    public static String toYearMonthDayFormat(Node node) {
        return toYearMonthDayFormat(node.getTextContent());
    }

    public static String toYearMonthDayFormat(String text) {
        var date = DateTime.parse(text);
        return yyyymmddPattern.print(date);
    }
}
