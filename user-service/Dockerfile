
# Copyright 2020 Crown Copyright
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
FROM openjdk:11@sha256:736dd07fc5cb53060a49d38c86b9e673974510c5a18afc40b1c936475f59aee7
VOLUME /tmp

ARG JAR_FILE

COPY $JAR_FILE /usr/share/user-service/user-service.jar

LABEL service="user" project="palisade"

ENTRYPOINT ["java", "-Dspring.profiles.active=k8s", "-classpath", "/usr/share/classpath", "-jar", "/usr/share/user-service/user-service.jar"]