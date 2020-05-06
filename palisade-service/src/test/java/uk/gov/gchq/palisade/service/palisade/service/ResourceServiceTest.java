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

package uk.gov.gchq.palisade.service.palisade.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.MethodRule;
import org.junit.runner.RunWith;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.request.GetResourcesByIdRequest;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.web.ResourceClient;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResourceServiceTest {
    @Rule
    public LogLevelRule logLevelRule = new LogLevelRule();
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private ResourceClient resourceClient = Mockito.mock(ResourceClient.class);
    private ResourceService resourceService;
    private Map<LeafResource, ConnectionDetail> resources = new HashMap<>();
    private ExecutorService executor;

    @Before
    public void setUp() {
        executor = Executors.newSingleThreadExecutor();
        logger = (Logger) LoggerFactory.getLogger(ResourceService.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        resourceService = new ResourceService(resourceClient, executor);
        FileResource resource = new FileResource().id("/path/to/bob_file.txt");
        ConnectionDetail connectionDetail = new SimpleConnectionDetail().uri("data-service");
        resources.put(resource, connectionDetail);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private List<String> getMessages(final Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    @LogLevel(packageToLevel = {"uk.gov.gchq.palisade.service.palisade.service.ResourceService=DEBUG"})
    public void infoOnGetResourcesRequestTest() {
        // Given
        GetResourcesByIdRequest request = new GetResourcesByIdRequest().resourceId("/path/to/bob_file.txt");
        request.setOriginalRequestId(new RequestId().id("Original ID"));
        Map<LeafResource, ConnectionDetail> response = Mockito.mock(Map.class);
        Mockito.when(resourceClient.getResourcesById(Mockito.eq(request))).thenReturn(response);

        // When
        resourceService.getResourcesById(request);

        // Then
        List<String> infoMessages = getMessages(event -> event.getLevel() == Level.DEBUG);

        MatcherAssert.assertThat(infoMessages, Matchers.hasItems(
                Matchers.containsString(request.getOriginalRequestId().getId()),
                Matchers.anyOf(
                        Matchers.containsString(response.toString()),
                        Matchers.containsString("Original ID"))
        ));

    }

    @Test
    public void getResourceByIdReturnsMappedResources() {
        //Given
        when(resourceClient.getResourcesById(Mockito.any(GetResourcesByIdRequest.class))).thenReturn(resources);

        //When
        GetResourcesByIdRequest request = new GetResourcesByIdRequest().resourceId("/path/to/bob_file.txt");
        request.setOriginalRequestId(new RequestId().id("Original ID"));
        CompletableFuture<Map<LeafResource, ConnectionDetail>> actual = resourceService.getResourcesById(request);

        //Then
        assertEquals(resources, actual.join());
    }

    /**
     * marks a method to use a different log level for the execution phase
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @Inherited
    public @interface LogLevel {
        String[] packageToLevel();
    }

    /**
     * a Junit Rule that check for LogLevel annotation on methods and activates the configured log level per package. After
     * the test was executed, restores the previous log level.
     */
    public static class LogLevelRule implements MethodRule {

        @Override
        public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {

            return new Statement() {
                @Override
                public void evaluate() throws Throwable {

                    // activate log level desired, remember what they were
                    Map<String, Level> existingPackageLogLevel = new HashMap<>();
                    LogLevel logLevelAnnotation = method.getAnnotation(LogLevel.class);
                    if (Optional.ofNullable(logLevelAnnotation).isPresent()) {
                        activate(logLevelAnnotation.packageToLevel(), existingPackageLogLevel);
                    }

                    // run the test
                    Throwable testFailure = evaluateSafely(base);

                    // revert the log level back to what it was
                    if (!existingPackageLogLevel.isEmpty()) {
                        deactivate(existingPackageLogLevel);
                    }

                    if (Optional.ofNullable(testFailure).isPresent()) {
                        throw testFailure;
                    }
                }

                /**
                 * execute the test safely so that if it fails, we can still revert the log level
                 */
                private Throwable evaluateSafely(final Statement base) {
                    try {
                        base.evaluate();
                        return null;
                    } catch (Throwable throwable) {
                        return throwable;
                    }
                }
            };
        }

        /**
         * activates the log level per package and remember the current setup
         *
         * @param packageToLevel          the configuration of the annotation
         * @param existingPackageLogLevel where to store the current information
         */
        protected void activate(final String[] packageToLevel, final Map<String, Level> existingPackageLogLevel) {
            for (String pkgToLevel : packageToLevel) {
                String[] split = pkgToLevel.split("=");
                String pkg = split[0];
                String levelString = split[1];
                Logger logger = (Logger) LoggerFactory.getLogger(pkg);
                Level level = logger.getEffectiveLevel();
                existingPackageLogLevel.put(pkg, level);
                logger.setLevel(Level.toLevel(levelString));
            }
        }

        /**
         * resets the log level of the changes packages back to what it was before
         *
         * @param existingPackageLogLevel is the package requiring log level adjustment
         */
        protected void deactivate(final Map<String, Level> existingPackageLogLevel) {
            existingPackageLogLevel.forEach((key, value) -> ((Logger) LoggerFactory.getLogger(key)).setLevel(value));
        }

    }

}
