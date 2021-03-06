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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.UserId;
import uk.gov.gchq.palisade.jsonserialisation.JSONSerialiser;
import uk.gov.gchq.palisade.policy.PassThroughRule;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.ConnectionDetail;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.palisade.repository.PersistenceLayer;
import uk.gov.gchq.palisade.service.palisade.request.RegisterDataRequest;
import uk.gov.gchq.palisade.service.palisade.web.PalisadeController;
import uk.gov.gchq.palisade.service.request.DataRequestConfig;
import uk.gov.gchq.palisade.service.request.DataRequestResponse;
import uk.gov.gchq.palisade.util.ResourceBuilder;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

@RunWith(MockitoJUnitRunner.class)
public class PalisadeServiceExceptionHandlerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimplePalisadeServiceTest.class);

    private final DataRequestConfig expectedConfig = new DataRequestConfig();
    private ResultAggregationService aggregationService;
    private AuditService auditService;
    private PersistenceLayer persistenceLayer;
    private PolicyService policyService;
    private ResourceService resourceService;
    private UserService userService;
    private SimplePalisadeService service;
    private CompletableFuture<DataRequestResponse> futureResponse = new CompletableFuture<>();
    private DataRequestResponse expectedResponse = new DataRequestResponse();
    private RegisterDataRequest dataRequest = new RegisterDataRequest();
    private DataRequestConfig dataRequestConfig = new DataRequestConfig();
    private RequestId originalRequestId = new RequestId().id("Bob");

    private User user;
    private Set<LeafResource> resources = new HashSet<>();
    private Map<LeafResource, Rules> rules = new HashMap<>();
    private PalisadeController controller;
    private MockMvc mvc;
    private ExecutorService executor;

    @Before
    public void setup() {
        executor = Executors.newSingleThreadExecutor();
        mockOtherServices();
        service = new SimplePalisadeService(auditService, userService, policyService, resourceService, persistenceLayer, executor, aggregationService);
        LOGGER.info("Simple Palisade Service created: {}", service);
        createExpectedDataConfig();
        user = new User().userId("Bob").roles("Role1", "Role2").auths("Auth1", "Auth2");

        ConnectionDetail connectionDetail = new SimpleConnectionDetail().uri("data-service");
        FileResource resource = ((FileResource) ResourceBuilder.create("file:/path/to/new/bob_file.txt"))
                .type("bob")
                .serialisedFormat("txt")
                .connectionDetail(connectionDetail);
        resources.add(resource);

        Rules rule = new Rules().rule("Rule1", new PassThroughRule());
        rules.put(resource, rule);


        dataRequest.userId(new UserId().id("Bob")).context(new Context().purpose("Testing")).resourceId("/path/to/new/bob_file.txt");
        dataRequestConfig.user(user).context(dataRequest.getContext()).rules(rules);
        dataRequestConfig.setOriginalRequestId(originalRequestId);
        expectedResponse.resources(resources);
        expectedResponse.originalRequestId(originalRequestId);
        futureResponse.complete(expectedResponse);
        controller = new PalisadeController(service, JSONSerialiser.createDefaultMapper());
        mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new PalisadeServiceExceptionHandler())
                .build();
    }

    private void createExpectedDataConfig() {
        User user = new User();
        Context context = new Context();
        Map<LeafResource, Rules> ruleMap = new HashMap<>();
        FileResource resource = new FileResource();
        Rules rules = new Rules();
        Set<String> roles = new HashSet<>();
        Set<String> auth = new HashSet<>();

        user.setUserId(new UserId().id("userId"));
        roles.add("roles");
        user.setRoles(roles);
        auth.add("auth");
        user.setAuths(auth);
        context.purpose("purpose");
        ruleMap.put(resource, rules);
        expectedConfig.setUser(user);
        expectedConfig.setContext(context);
        expectedConfig.setRules(ruleMap);
    }

    public static String asJsonString(final Object obj) {
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void verifyNullPointerException() throws Exception {
        // Given
        RegisterDataRequest registerDataRequest = new RegisterDataRequest();
        UserId userId = new UserId();
        userId.setId("1234");
        registerDataRequest.setUserId(userId);
        Context context = new Context();
        Map<String, Object> mapStringObj = new HashMap<>();
        mapStringObj.put("key1", 789);
        mapStringObj.put("key2", 3456);
        context.setContents(mapStringObj);
        context.purpose("testing");
        registerDataRequest.setContext(context);
        registerDataRequest.setResourceId("0345");

        // When
        MockHttpServletResponse response = mvc.perform(MockMvcRequestBuilders
                .post("/registerDataRequest")
                .content(asJsonString(registerDataRequest))
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andReturn().getResponse();

        // Then
        assertEquals(response.getStatus(), HttpStatus.BAD_REQUEST.value());
        assertThat(response.getContentAsString(), containsString("class java.lang.NullPointerException"));
    }

    private void mockOtherServices() {
        auditService = Mockito.mock(AuditService.class);
        policyService = Mockito.mock(PolicyService.class);
        resourceService = Mockito.mock(ResourceService.class);
        userService = Mockito.mock(UserService.class);
        aggregationService = Mockito.mock(ResultAggregationService.class);
        persistenceLayer = Mockito.mock(PersistenceLayer.class);
    }

}