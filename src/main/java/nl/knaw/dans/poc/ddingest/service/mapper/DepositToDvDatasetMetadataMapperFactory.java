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
package nl.knaw.dans.poc.ddingest.service.mapper;

import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
public class DepositToDvDatasetMetadataMapperFactory {

    private final Map<String, String> iso1ToDataverseLanguage;
    private final Map<String, String> iso2ToDataverseLanguage;

    public DepositToDvDatasetMetadataMapperFactory(Map<String, String> iso1ToDataverseLanguage, Map<String, String> iso2ToDataverseLanguage) {
        this.iso1ToDataverseLanguage = iso1ToDataverseLanguage;
        this.iso2ToDataverseLanguage = iso2ToDataverseLanguage;
    }

    public DepositToDvDatasetMetadataMapper createMapper(boolean deduplicate) {
        return new DepositToDvDatasetMetadataMapper(
            deduplicate,
            getActiveMetadataBlocks(),
            iso1ToDataverseLanguage,
            iso2ToDataverseLanguage
        );
    }

    Set<String> getActiveMetadataBlocks() {
        return Set.of("citation", "dansRights", "dansRelationMetadata", "dansArchaeologyMetadata", "dansTemporalSpatial", "dansDataVaultMetadata");
    }
}
