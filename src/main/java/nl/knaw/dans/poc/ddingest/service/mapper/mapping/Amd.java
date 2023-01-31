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
import org.apache.commons.lang3.StringUtils;
import nl.knaw.dans.poc.depositmanager.XPathEvaluator;
import org.w3c.dom.Node;

import java.util.Optional;

import static nl.knaw.dans.poc.ddingest.DepositState.PUBLISHED;
import static nl.knaw.dans.poc.ddingest.DepositState.SUBMITTED;

@Slf4j
public class Amd extends Base {

    public static Optional<String> toDateOfDeposit(Node node) {
        var result = getFirstChangeToState(node, SUBMITTED.name());

        if (result.isEmpty()) {
            return toPublicationDate(node);
        }

        return result;
    }

    public static Optional<String> toPublicationDate(Node node) {
        var result = getFirstChangeToState(node, PUBLISHED.name());

        if (result.isEmpty()) {
            return getChildNode(node, "//lastStateChange").map(Base::toYearMonthDayFormat);
        }

        return result;
    }

    static Optional<String> getFirstChangeToState(Node node, String state) {
        return XPathEvaluator.nodes(node, "//stateChangeDates/damd:stateChangeDate")
            .filter(n -> {
                var toState = getChildNode(n, "toState")
                    .map(Node::getTextContent)
                    .orElse("");

                return toState.equals(state);
            })
            .map(n -> getChildNode(n, "changeDate").map(Node::getTextContent))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(StringUtils::isNotBlank)
            .map(Base::toYearMonthDayFormat)
            .sorted()
            .findFirst();

    }
}
