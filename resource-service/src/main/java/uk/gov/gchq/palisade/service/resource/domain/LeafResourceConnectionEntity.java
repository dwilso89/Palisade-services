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

import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ConnectionDetail;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Index;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Entity
@Table(name = "leaf_resource_connection",
        uniqueConstraints = {@UniqueConstraint(columnNames = "resource_id, connection_detail")},
        indexes = {@Index(name = "leaf_resource_id", columnList = "resource_id"),
                @Index(name = "leaf_resource_serialised_format", columnList = "serialised_format, resource_id"),
                @Index(name = "leaf_resource_type", columnList = "type, resource_id")})
public class LeafResourceConnectionEntity {

    @Id
    @Column(name = "resource_id", columnDefinition = "varchar(1024)")
    private String resourceId;

    @Column(name = "leaf_resource", columnDefinition = "clob")
    @Convert(converter = LeafResourceConverter.class)
    private LeafResource leafResource;

    @Column(name = "parent_ids", columnDefinition = "clob")
    @Convert(converter = ParentIdsConverter.class)
    private List<String> parentIds;

    @Column(name = "connection_detail", columnDefinition = "clob")
    @Convert(converter = ConnectionDetail.class)
    private ConnectionDetail connectionDetail;

    @Column(name = "serialised_format", columnDefinition = "varchar(255)")
    private String serialisedFormat;

    @Column(name = "type", columnDefinition = "varchar(255)")
    private String type;

    public LeafResourceConnectionEntity(final LeafResource leafResource, final ConnectionDetail connectionDetail) {
        this.resourceId = requireNonNull(leafResource, "leafResource").getId();
        this.leafResource = leafResource;
        // populate parentIds
        this.connectionDetail = connectionDetail;
        this.serialisedFormat = leafResource.getSerialisedFormat();
        this.type = leafResource.getType();
    }

    public LeafResourceConnectionEntity() {
    }

    public SimpleImmutableEntry<LeafResource, ConnectionDetail> leafResourceConnectionDetail() {
        return new SimpleImmutableEntry<>(this.leafResource, this.connectionDetail);
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(final String resourceId) {
        this.resourceId = resourceId;
    }

    public LeafResource getLeafResource() {
        return leafResource;
    }

    public void setLeafResource(final LeafResource leafResource) {
        this.leafResource = leafResource;
    }

    public List<String> getParentIds() {
        return parentIds;
    }

    public void setParentIds(List<String> parentIds) {
        this.parentIds = parentIds;
    }

    public ConnectionDetail getConnectionDetail() {
        return connectionDetail;
    }

    public void setConnectionDetail(final ConnectionDetail connectionDetail) {
        this.connectionDetail = connectionDetail;
    }

    public String getSerialisedFormat() {
        return serialisedFormat;
    }

    public void setSerialisedFormat(final String serialisedFormat) {
        this.serialisedFormat = serialisedFormat;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
