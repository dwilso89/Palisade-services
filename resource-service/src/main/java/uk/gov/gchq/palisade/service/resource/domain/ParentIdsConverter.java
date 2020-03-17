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

import javax.persistence.AttributeConverter;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class ParentIdsConverter implements AttributeConverter<List<String>, String> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ParentIdsConverter.class);

    private final ObjectMapper objectMapper;

    public ParentIdsConverter(final ObjectMapper objectMapper) {
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public String convertToDatabaseColumn(List<String> strings) {
        if (Optional.ofNullable(strings).isEmpty()) {
            return null;
        }
        try {
            return this.objectMapper.writeValueAsString(strings);
        } catch (JsonProcessingException e) {
            LOGGER.error("Could not convert parent id list to json string.", e);
            return null;
        }
    }

    @Override
    public List<String> convertToEntityAttribute(String s) {
        if (Optional.ofNullable(s).isPresent()) {
            try {
                return this.objectMapper.readValue(s, List.class);
            } catch (IOException e) {
                LOGGER.error("Conversion error whilst trying to convert string(JSON) to list of parent ids.", e);
            }
        }
        return Collections.emptyList();
    }
}
