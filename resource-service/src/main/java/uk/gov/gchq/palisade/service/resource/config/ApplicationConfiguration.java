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
package uk.gov.gchq.palisade.service.resource.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import uk.gov.gchq.palisade.data.serialise.AvroSerialiser;
import uk.gov.gchq.palisade.data.serialise.Serialiser;
import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.service.ResourceService;
import uk.gov.gchq.palisade.service.resource.domain.ResourceConverter;
import uk.gov.gchq.palisade.service.resource.exception.ApplicationAsyncExceptionHandler;
import uk.gov.gchq.palisade.service.resource.repository.JpaPersistenceLayer;
import uk.gov.gchq.palisade.service.resource.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.resource.repository.ResourceRepository;
import uk.gov.gchq.palisade.service.resource.repository.SerialisedFormatRepository;
import uk.gov.gchq.palisade.service.resource.repository.TypeRepository;
import uk.gov.gchq.palisade.service.resource.service.ConfiguredHadoopResourceService;
import uk.gov.gchq.palisade.service.resource.service.HadoopResourceService;
import uk.gov.gchq.palisade.service.resource.service.SimpleResourceService;
import uk.gov.gchq.palisade.service.resource.service.StreamingResourceServiceProxy;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.Executor;

/**
 * Bean configuration and dependency injection graph
 */
@Configuration
public class ApplicationConfiguration implements AsyncConfigurer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationConfiguration.class);

    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resource", havingValue = "json")
    public JsonResourceConfiguration resourceConfiguration() {
        LOGGER.info("Standard Policy Configuration Instantiated");
        return new JsonResourceConfiguration();
    }

    @Bean
    @ConditionalOnProperty(prefix = "population", name = "resource", havingValue = "json")
    public JsonResourcePrepopulationFactory resourcePrepopulationFactory() {
        LOGGER.info("Standard Policy Cache Warmer Instantiated");
        return new JsonResourcePrepopulationFactory();
    }

    @Bean(name = "jpa-persistence")
    public JpaPersistenceLayer persistenceLayer(final ResourceRepository resourceRepository, final TypeRepository typeRepository, final SerialisedFormatRepository serialisedFormatRepository, final @Qualifier("impl") ResourceService delegate) {
        return new JpaPersistenceLayer(resourceRepository, typeRepository, serialisedFormatRepository);
    }

    @Bean
    public ResourceConverter resourceConverter() {
        return new ResourceConverter();
    }

    @Bean
    public Serialiser<LeafResource> avroSerialiser() {
        return new AvroSerialiser<>(LeafResource.class);
    }

    @Bean
    public StreamingResourceServiceProxy resourceServiceProxy(final PersistenceLayer persistenceLayer, final @Qualifier("impl") ResourceService delegate, final Serialiser<LeafResource> serialiser) {
        return new StreamingResourceServiceProxy(persistenceLayer, delegate, serialiser);
    }

    @Bean("simpleResourceService")
    @ConditionalOnProperty(prefix = "resource", name = "implementation", havingValue = "simple")
    @Qualifier("impl")
    public ResourceService simpleResourceService() {
        return new SimpleResourceService();
    }

    @Bean("hadoopResourceService")
    @ConditionalOnProperty(prefix = "resource", name = "implementation", havingValue = "hadoop")
    @Qualifier("impl")
    public HadoopResourceService hadoopResourceService(final org.apache.hadoop.conf.Configuration config) throws IOException {
        return new ConfiguredHadoopResourceService(config);
    }

    @Bean
    public org.apache.hadoop.conf.Configuration hadoopConfiguration() {
        return new org.apache.hadoop.conf.Configuration();
    }

    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return JSONSerialiser.createDefaultMapper();
    }

    @Override
    @Bean("threadPoolTaskExecutor")
    public Executor getAsyncExecutor() {
        return Optional.of(new ThreadPoolTaskExecutor()).stream().peek(ex -> {
            ex.setThreadNamePrefix("AppThreadPool-");
            ex.setCorePoolSize(6);
            LOGGER.info("Starting ThreadPoolTaskExecutor with core = [{}] max = [{}]", ex.getCorePoolSize(), ex.getMaxPoolSize());
        }).findFirst().orElse(null);
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return new ApplicationAsyncExceptionHandler();
    }

}
