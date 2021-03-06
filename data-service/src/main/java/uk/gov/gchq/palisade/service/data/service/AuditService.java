/*
 * Copyright 2019 Crown Copyright
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
package uk.gov.gchq.palisade.service.data.service;

import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.service.Service;
import uk.gov.gchq.palisade.service.data.request.AuditRequest;
import uk.gov.gchq.palisade.service.data.web.AuditClient;

import java.net.URI;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class AuditService implements Service {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuditService.class);
    private final AuditClient client;
    private final Supplier<URI> uriSupplier;
    private final Executor executor;

    public AuditService(final AuditClient auditClient, final Supplier<URI> uriSupplier, final Executor executor) {
        this.client = auditClient;
        this.uriSupplier = uriSupplier;
        this.executor = executor;
    }

    public Boolean audit(final AuditRequest request) {
        LOGGER.debug("Submitting audit to audit service: {}", request);

        Boolean response;
        try {
            LOGGER.info("Audit request: {}", request);
            URI clientUri = this.uriSupplier.get();
            LOGGER.debug("Using client uri: {}", clientUri);
            response = this.client.audit(clientUri, request);
            LOGGER.info("Audit response: {}", response);
        } catch (Exception ex) {
            LOGGER.error("Failed to log audit request: {}", ex.getMessage());
            throw new RuntimeException(ex);
        }

        return response;
    }

    public Response getHealth() {
        try {
            URI clientUri = this.uriSupplier.get();
            LOGGER.debug("Using client uri: {}", clientUri);
            return this.client.getHealth(clientUri);
        } catch (Exception ex) {
            LOGGER.error("Failed to get health: {}", ex.getMessage());
            throw new RuntimeException(ex); //rethrow the exception
        }
    }
}
