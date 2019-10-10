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

package uk.gov.gchq.palisade.service.resource.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class ResourceDetailsTest {

    private static final String FILENAME = "/home/user/Docs/other_file.json";
    private static final String TYPE = "other";
    private static final String FORMAT = "json";

    private ResourceDetails expected = new ResourceDetails(FILENAME, TYPE, FORMAT);

    /*@Before
    public void setup() {

    }*/

    @Test
    public void getReourceDetailsTest() {
        //When
        ResourceDetails details = ResourceDetails.getResourceDetailsFromFileName("/home/user/Docs/other_file.json");

        //Then
        assertEquals(expected, details);
    }
}
