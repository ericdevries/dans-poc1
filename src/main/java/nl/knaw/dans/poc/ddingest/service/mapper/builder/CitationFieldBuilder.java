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
package nl.knaw.dans.poc.ddingest.service.mapper.builder;

import nl.knaw.dans.lib.dataverse.model.user.AuthenticatedUser;
import org.w3c.dom.Node;

import java.util.function.Function;
import java.util.stream.Stream;

import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.ALTERNATIVE_TITLE;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.AUTHOR;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.CONTRIBUTOR;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.DATASET_CONTACT;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.DATA_SOURCES;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.DATE_OF_COLLECTION;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.DATE_OF_DEPOSIT;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.DESCRIPTION;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.DISTRIBUTION_DATE;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.DISTRIBUTOR;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.GRANT_NUMBER;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.KEYWORD;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.LANGUAGE;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.OTHER_ID;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.PRODUCTION_DATE;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.PUBLICATION;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.SUBJECT;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.TITLE;

public class CitationFieldBuilder extends FieldBuilder {

    public void addTitle(Stream<String> nodes) {
        addSingleString(TITLE, nodes);
    }

    public void addOtherIds(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(OTHER_ID, stream, generator);
    }

    public void addOtherIdsStrings(Stream<String> stream, CompoundFieldGenerator<String> generator) {
        addMultipleString(OTHER_ID, stream, generator);
    }

    public void addAuthors(Stream<Node> creators, CompoundFieldGenerator<Node> generator) {
        addMultiple(AUTHOR, creators, generator);
    }

    public void addAlternativeTitle(Stream<String> stream) {
        addSingleString(ALTERNATIVE_TITLE, stream);
    }

    public void addDescription(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(DESCRIPTION, stream, generator);
    }

    public void addSubject(Stream<String> stream, Function<String, String> mapper) {
        addMultipleControlledFields(SUBJECT, stream.map(mapper));
    }

    public void addKeywords(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(KEYWORD, stream, generator);
    }

    public void addPublications(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(PUBLICATION, stream, generator);
    }

    public void addLanguages(Stream<Node> stream, Function<Node, String> mapper) {
        addMultipleControlledFields(LANGUAGE, stream.map(mapper));
    }

    public void addProductionDate(Stream<String> stream) {
        addSingleString(PRODUCTION_DATE, stream);
    }

    public void addContributors(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(CONTRIBUTOR, stream, generator);
    }

    public void addGrantNumbers(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(GRANT_NUMBER, stream, generator);
    }

    public void addDistributor(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(DISTRIBUTOR, stream, generator);
    }

    public void addDistributionDate(Stream<String> stream) {
        addSingleString(DISTRIBUTION_DATE, stream);
    }

    public void addDateOfCollections(Stream<Node> stream, CompoundFieldGenerator<Node> generator) {
        addMultiple(DATE_OF_COLLECTION, stream, generator);
    }

    public void addDataSources(Stream<String> dataSources) {
        addSingleString(DATA_SOURCES, dataSources);
    }

    public void addDatasetContact(Stream<AuthenticatedUser> data, CompoundFieldGenerator<AuthenticatedUser> generator) {
        data.forEach(value -> {
            var builder = getCompoundBuilder(DATASET_CONTACT, true);
            generator.build(builder, value);
        });
    }

    public void addDateOfDeposit(String value) {
        addSingleString(DATE_OF_DEPOSIT, Stream.ofNullable(value));
    }
}
