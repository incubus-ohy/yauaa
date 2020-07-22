/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2020 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.parse.useragent.quarkus;

import nl.basjes.parse.useragent.UserAgentAnalyzer;

import javax.annotation.PostConstruct;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class ParseService {

    private UserAgentAnalyzer userAgentAnalyzer = null;

    @PostConstruct
    public void automaticStartup() {
        userAgentAnalyzer = UserAgentAnalyzer.newBuilder()
            .immediateInitialization()
            .build();
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getYamlGET(@HeaderParam("user-agent") String userAgentString
    ) {
        return doYaml(userAgentString);
    }

    private String doYaml(String userAgentString) {
        if (userAgentString == null) {
            return "";
        }

        return userAgentAnalyzer.parse(userAgentString).toYamlTestCase();
    }

}