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
import nl.knaw.dans.poc.ddingest.service.mapper.builder.CompoundFieldGenerator;
import org.w3c.dom.Node;

import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.SPATIAL_BOX_EAST;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.SPATIAL_BOX_NORTH;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.SPATIAL_BOX_SCHEME;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.SPATIAL_BOX_SOUTH;
import static nl.knaw.dans.poc.ddingest.service.DepositDatasetFieldNames.SPATIAL_BOX_WEST;

@Slf4j
public class SpatialBox extends Spatial {
    public static CompoundFieldGenerator<Node> toEasyTsmSpatialBoxValueObject = (builder, node) -> {
        var envelope = getChildNode(node, "gml:Envelope")
            .orElseThrow(() -> new IllegalArgumentException("Missing gml:Envelope node"));

        var isRd = isRd(envelope);

        var lowerCorner = getChildNode(envelope, "gml:lowerCorner")
            .map(n -> getPoint(n, isRd))
            .orElseThrow(() -> new IllegalArgumentException("Missing gml:lowerCorner node in gml:Envelope"));

        var upperCorner = getChildNode(envelope, "gml:upperCorner")
            .map(n -> getPoint(n, isRd))
            .orElseThrow(() -> new IllegalArgumentException("Missing gml:upperCorner node in gml:Envelope"));

        builder.addControlledSubfield(SPATIAL_BOX_SCHEME, isRd ? RD_SCHEME : LONLAT_SCHEME);
        builder.addSubfield(SPATIAL_BOX_NORTH, upperCorner.getY());
        builder.addSubfield(SPATIAL_BOX_EAST, upperCorner.getX());
        builder.addSubfield(SPATIAL_BOX_SOUTH, lowerCorner.getY());
        builder.addSubfield(SPATIAL_BOX_WEST, lowerCorner.getX());
    };

}
