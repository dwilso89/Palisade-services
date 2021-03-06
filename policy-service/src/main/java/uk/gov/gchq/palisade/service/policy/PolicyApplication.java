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

package uk.gov.gchq.palisade.service.policy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

import java.util.Arrays;

@EnableEurekaClient
@EnableFeignClients
@EnableCaching
@SpringBootApplication
public class PolicyApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(PolicyApplication.class);

    public static void main(final String[] args) {
        LOGGER.debug("PolicyApplication started with: {} {} {}", PolicyApplication.class.toString(), "main", Arrays.toString(args));
        new SpringApplicationBuilder(PolicyApplication.class).web(WebApplicationType.SERVLET)
                .run(args);
    }

}
