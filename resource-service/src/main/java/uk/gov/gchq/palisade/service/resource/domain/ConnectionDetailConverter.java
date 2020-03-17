/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.gov.gchq.palisade.service.resource.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.gov.gchq.palisade.service.ConnectionDetail;

import javax.persistence.AttributeConverter;

import java.io.IOException;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class ConnectionDetailConverter implements AttributeConverter<ConnectionDetail, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionDetailConverter.class);

    private final ObjectMapper objectMapper;

    public ConnectionDetailConverter(final ObjectMapper objectMapper) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public String convertToDatabaseColumn(final ConnectionDetail connectionDetail) {
        if (Optional.ofNullable(connectionDetail).isEmpty()) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsString(connectionDetail);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not convert connectionDetail to json string.", e);
            return null;
        }
    }

    @Override
    public ConnectionDetail convertToEntityAttribute(final String json) {
        if (Optional.ofNullable(json).isPresent()) {
            try {
                return this.objectMapper.readValue(json, ConnectionDetail.class);
            } catch (IOException e) {
                LOGGER.error("Conversion error whilst trying to convert string(JSON) to list of parent ids.", e);
            }
        }
        return () -> "";
    }
}
