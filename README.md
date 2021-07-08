# Let’s create a Spring Boot project named graphql-wrapper, using the Gradle build tool and the Spring Web MVC dependency.

spring init --build=gradle --dependencies=web graphql-wrapper
The resulting boilerplate build.gradle is as follows:

plugins {
    id 'org.springframework.boot' version '2.3.3.RELEASE'
    id 'io.spring.dependency-management' version '1.0.10.RELEASE'
    id 'java'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    testImplementation('org.springframework.boot:spring-boot-starter-test') {
        exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
    }
}

test {
    useJUnitPlatform()
}
Add org.springframework.boot:spring-boot-starter-validation and io.swagger:swagger-annotations:1.5.21 as dependencies. The Swagger Codegen models will use the @Valid annotation from spring-boot-starter-validation, and swagger-annotations provides Swagger annotations (e.g., @ApiOperation) for use in the controller.

dependencies {
    // ...
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    // ...
    // Swagger Annotations
    compile 'io.swagger:swagger-annotations:1.5.21'
    // ...
}
Create src/main/resources/application.yaml to house application properties. (We’ll use a YAML properties file instead of the default application.properties file.)

server:
  # Serve the application at 8080. 
  port: 8080
  servlet:
    # Prefix all routes with /api.
    contextPath: /api
Create OpenAPI Spec
Manually creating the OpenAPI spec from the GraphQL schema is relatively time-consuming. I have not yet found or written a script that automates this task, so I resort to a manual process involving regular expressions, multiple cursors, and find/replace. Perhaps automating this task will be the subject of a future blog post.

Create src/main/resources/public/api-spec.yaml. Add a single GET path for /cities/{city-name} that will return a City component. Define components based on the objects in the SCHEMA tab in the GraphQL Playground at https://graphql-weather-api.herokuapp.com/:


openapi: 3.0.1
info:
  title: GraphQL Wrapper
  description: Provides a REST wrapper for a GraphQL service.
  version: "1.0.0"
servers:
  - url: /api/v1

paths:
  /cities/{city-name}:
    get:
      operationId: getCityByName
      parameters:
        - $ref: "#/components/parameters/Name"
      responses:
        200:
          description: City
          content:
            application/json:
              schema:
                $ref: "#/components/schemas/City"

components:
  parameters:
    Name:
      name: city-name
      description: "Example: Minneapolis."
      in: path
      required: true
      schema:
        type: string

  schemas:
    City:
      type: object
      properties:
        id:
          type: string
        name:
          type: string
        country:
          type: string
        coord:
          $ref: "#/components/schemas/Coordinates"
        weather:
          $ref: "#/components/schemas/Weather"

    Clouds:
      type: object
      properties:
        all:
          type: integer
          format: int32
        visibility:
          type: integer
          format: int32
        humidity:
          type: integer
          format: int32

    Coordinates:
      type: object
      properties:
        lon:
          type: number
          format: float
        lat:
          type: number
          format: float

    Summary:
      type: object
      properties:
        title:
          type: string
        description:
          type: string
        icon:
          type: string

    Temperature:
      type: object
      properties:
        actual:
          type: number
          format: float
        feelsLike:
          type: number
          format: float
        min:
          type: number
          format: float
        max:
          type: number
          format: float

    Weather:
      type: object
      properties:
        summary:
          $ref: "#/components/schemas/Summary"
        temperature:
          $ref: "#/components/schemas/Temperature"
        wind:
          $ref: "#/components/schemas/Wind"
        clouds:
          $ref: "#/components/schemas/Clouds"
        timestamp:
          type: integer
          format: int32

    Wind:
      type: object
      properties:
        speed:
          type: number
          format: float
        deg:
          type: integer
          format: int32
Add Swagger Codegen
In build.gradle, add the org.hidetake.swagger.generator plugin:

plugins {
    // ...
    // Swagger Codegen
    id 'org.hidetake.swagger.generator' version '2.18.2'
    // ...
}
Also in build.gradle, add the swagger-codegen-cli dependency:

dependencies {
    // ...
    // Swagger Codegen
    swaggerCodegen 'io.swagger.codegen.v3:swagger-codegen-cli:3.0.21'
    // ...
}
And yet again in build.gradle, add the following:

// Swagger Codegen: Define output directory for generated code.
def generatedSourceOutputDir = "${buildDir}/generated-source"

// Swagger Codegen: Configure.
swaggerSources {
    openapi {
        // OpenAPI spec location.
        inputFile = file('src/main/resources/public/api-spec.yaml')
        code {
            language = 'spring'
            // Generate only models (vs. components, controllers, etc.).
            components = ['models']
            // Config file location.
            configFile = file('swagger-codegen-config.json')
            outputDir = file("${generatedSourceOutputDir}")
        }
    }
}

// Swagger Codegen: Attach the generated directories as a source for the project.
sourceSets.main.java.srcDir "${generatedSourceOutputDir}/src/main/java"
sourceSets.main.resources.srcDir "${generatedSourceOutputDir}/src/main/resources"
Create swagger-codegen-config.json, indicating the following:

This is a Spring MVC project.
Generated models should be placed in the com.example.graphqlwrapper.model package (in generatedSourceOutputDir as defined above).
The application’s main method is in com.example.graphqlwrapper.
{
  "library": "spring-mvc",
  "modelPackage": "com.example.graphqlwrapper.model",
  "invokerPackage": "com.example.graphqlwrapper"
}
Run the following to generate models in build/generated-source/src/main/java/com.example.graphqlwrapper.model:

./gradlew generateSwaggerCode
Add Manifold GraphQL
Add Manifold as a dependency in build.gradle and add configuration details:

dependencies {
    // ...
    // Manifold GraphQL
    implementation "systems.manifold:manifold-graphql:2020.1.23"
    annotationProcessor "systems.manifold:manifold-graphql:2020.1.23"
    // ...
}

// ...

// Manifold GraphQL: Configure.
tasks.withType(JavaCompile) {
    options.compilerArgs += ['-Xplugin:Manifold']
}
tasks.compileJava {
    // Add build/resources/main to javac's classpath.
    classpath += files(sourceSets.main.output.resourcesDir)
    dependsOn processResources
}
tasks.compileTestJava {
    // Add build/resources/test to test javac's classpath.
    classpath += files(sourceSets.test.output.resourcesDir)
    dependsOn processTestResources
}
Add src/main/resources/graphql/schema.graphql by copying the text from the SCHEMA tab in the GraphQL Playground at https://graphql-weather-api.herokuapp.com/. Although we won’t directly use the schema file in this demo, the types can be used in the application by importing graphql.schema.

directive @cacheControl(
  maxAge: Int
  scope: CacheControlScope
) on FIELD_DEFINITION | OBJECT | INTERFACE
directive @specifiedBy(url: String!) on SCALAR
enum CacheControlScope {
  PUBLIC
  PRIVATE
}

type City {
  id: ID
  name: String
  country: String
  coord: Coordinates
  weather: Weather
}

type Clouds {
  all: Int
  visibility: Int
  humidity: Int
}

input ConfigInput {
  units: Unit
  lang: Language
}

type Coordinates {
  lon: Float
  lat: Float
}

enum Language {
  af
  al
  ar
  az
  bg
  ca
  cz
  da
  de
  el
  en
  eu
  fa
  fi
  fr
  gl
  he
  hi
  hr
  hu
  id
  it
  ja
  kr
  la
  lt
  mk
  no
  nl
  pl
  pt
  pt_br
  ro
  ru
  sv
  se
  sk
  sl
  sp
  es
  sr
  th
  tr
  ua
  uk
  vi
  zh_cn
  zh_tw
  zu
}

type Query {
  getCityByName(name: String!, country: String, config: ConfigInput): City
  getCityById(id: [String!], config: ConfigInput): [City]
}

type Summary {
  title: String
  description: String
  icon: String
}

type Temperature {
  actual: Float
  feelsLike: Float
  min: Float
  max: Float
}

enum Unit {
  metric
  imperial
  kelvin
}

scalar Upload

type Weather {
  summary: Summary
  temperature: Temperature
  wind: Wind
  clouds: Clouds
  timestamp: Int
}

type Wind {
  speed: Float
  deg: Int
}
Add src/main/resources/graphql/query.graphql. Creating the query file is currently a manual process. Similar to creating the OpenAPI spec, I use regular expressions, multiple cursors, and find/replace to convert the schema definitions into query fragments, then add any query definitions. Fragments are unnecessary for this demo; with dealing with larger schemas, though, they can dramatically reduce duplication.

query GetCityByName($name: String!) {
  getCityByName(name: $name) {
    ...City
  }
}

fragment City on City {
  id
  name
  country
  coord {
    ...Coordinates
  }
  weather {
    ...Weather
  }
}

fragment Clouds on Clouds {
  all
  visibility
  humidity
}

fragment Coordinates on Coordinates {
  lon
  lat
}

fragment Summary on Summary {
  title
  description
  icon
}

fragment Temperature on Temperature {
  actual
  feelsLike
  min
  max
}

fragment Weather on Weather {
  summary {
    ...Summary
  }
  temperature {
    ...Temperature
  }
  wind {
    ...Wind
  }
  clouds {
    ...Clouds
  }
  timestamp
}

fragment Wind on Wind {
  speed
  deg
}
Add Swagger UI
We’ll provide a user-friendly Swagger UI (via springdoc-openapi-ui) for serving our REST API. Add the org.springdoc:springdoc-openapi-ui:1.4.3 dependency in build.gradle.

dependencies {
    // ...
    // Springdoc OpenAPI UI
    implementation 'org.springdoc:springdoc-openapi-ui:1.4.3'
    // ...o.swagger:swagger-annotations:1.5.21"
}
In src/main/resources/application.yaml, specify the OpenAPI spec location:

# ...
springdoc:
  swagger-ui:
    # Specify the OpenAPI spec location.
    url: /api/api-spec.yaml
# ...
Add Spotless
For code formatting, add Spotless in build.gradle:

plugins {
    // ...
    // Spotless
    id "com.diffplug.spotless" version "5.3.0"
    // ...
}

// ...

// Spotless: Configure.
spotless {
    java {
        googleJavaFormat()
        importOrder 'java', 'javax', 'org', 'com', 'io'
        removeUnusedImports()
    }
}
Define Gradle Convenience Task
Add a convenience Gradle task in build.gradle for rebuilding the project (along with regenerating models, reformatting code, and running our unit tests) by the ./gradlew golden command:

task golden {
    dependsOn clean
    dependsOn generateSwaggerCode
    dependsOn spotlessJavaApply
    dependsOn test
    tasks.findByName('generateSwaggerCode').mustRunAfter clean
    tasks.findByName('spotlessJavaApply').mustRunAfter generateSwaggerCode
    tasks.findByName('test').mustRunAfter generateSwaggerCode
}
Add Service & Controller Files
Add the service and controller files. We’ll include rudimentary logging statements throughout for ease of debugging and tracing the data flow.

Add a QueryService interface and implementation to encapsulate GraphQL/Manifold code. Manifold lets us import graphql.query and provides a builder and models based on the GraphQL query.

src/main/java/com.example.graphqlwrapper/service/QueryService:

package com.example.graphqlwrapper.service;

public interface QueryService {
  Object getCityByName(String cityName);
}
src/main/java/com.example.graphqlwrapper/service/QueryServiceImpl:

package com.example.graphqlwrapper.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import graphql.query;

@Service("queryService")
public class QueryServiceImpl implements QueryService {
  private static final Logger LOG = LoggerFactory.getLogger(QueryServiceImpl.class);

  private static final String GRAPHQL_ENDPOINT = "https://graphql-weather-api.herokuapp.com/";

  public Object getCityByName(String cityName) {
    query.GetCityByName builder = query.GetCityByName.builder(cityName).build();
    query.GetCityByName.Result result = builder.request(GRAPHQL_ENDPOINT).post();
    LOG.info("result.getBindings().toJson(): {}", result.getBindings().toJson());
    return result.getBindings().get("getCityByName");
  }
}
Add a WeatherService interface and implementation that maps the QueryService result to the expected ResponseEntity. Note that City was generated by Swagger Codegen from the OpenAPI spec.

src/main/java/com.example.graphqlwrapper/service/WeatherService:

package com.example.graphqlwrapper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;

import com.example.graphqlwrapper.model.City;

public interface WeatherService {
    ResponseEntity<City> getCityByName(String cityName) throws JsonProcessingException;
}
src/main/java/com.example.graphqlwrapper/service/WeatherServiceImpl:

package com.example.graphqlwrapper.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.example.graphqlwrapper.model.City;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service("weatherService")
public class WeatherServiceImpl implements WeatherService {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherServiceImpl.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private final QueryService queryService;

    @Autowired
    WeatherServiceImpl(QueryService queryService) {
        this.queryService = queryService;
    }

    public ResponseEntity<City> getCityByName(String cityName) throws JsonProcessingException {
        Object result = queryService.getCityByName(cityName);
        String resultAsString = mapper.writeValueAsString(result);
        City city = mapper.readValue(resultAsString, City.class);
        LOG.info("city: {}", city);
        return new ResponseEntity<>(city, HttpStatus.OK);
    }
}
Add a WeatherController interface and implementation to define our REST endpoint and map it to our WeatherService.

src/main/java/com.example.graphqlwrapper/controller/WeatherController:

package com.example.graphqlwrapper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import com.example.graphqlwrapper.model.City;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

@Api(value = "v1")
public interface WeatherController {
    @ApiOperation(value = "", nickname = "getCityByName", response = City.class)
    @GetMapping(value = "/v1/cities/{cityName}", produces = "application/json")
    ResponseEntity<City> getCityByName(@PathVariable String cityName) throws JsonProcessingException;
}
src/main/java/com.example.graphqlwrapper/controller/WeatherControllerImpl:

package com.example.graphqlwrapper.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;

import com.example.graphqlwrapper.model.City;
import com.example.graphqlwrapper.service.WeatherService;

@Controller
public class WeatherControllerImpl implements WeatherController {
    private static final Logger LOG = LoggerFactory.getLogger(WeatherControllerImpl.class);

    private final WeatherService weatherService;

    @Autowired
    WeatherControllerImpl(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    public ResponseEntity<City> getCityByName(String cityName) throws JsonProcessingException {
        LOG.info("cityName: {}", cityName);
        return weatherService.getCityByName(cityName);
    }
}
Review Files
For ease of reference, here is our final build.gradle:

plugins {
    id 'org.springframework.boot' version '2.3.3.RELEASE'
    id 'io.spring.dependency-management' version '1.0.10.RELEASE'
    id 'java'

    // Spotless
    id "com.diffplug.spotless" version "5.3.0"

    // Swagger Codegen
    id 'org.hidetake.swagger.generator' version '2.18.2'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'
sourceCompatibility = '11'

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    testImplementation('org.springframework.boot:spring-boot-starter-test') {
        exclude group: 'org.junit.vintage', module: 'junit-vintage-engine'
    }

    // Manifold GraphQL
    implementation "systems.manifold:manifold-graphql:2020.1.23"
    annotationProcessor "systems.manifold:manifold-graphql:2020.1.23"

    // Springdoc OpenAPI UI
    implementation 'org.springdoc:springdoc-openapi-ui:1.4.6'

    // Swagger Annotations
    compile 'io.swagger:swagger-annotations:1.5.21'

    // Swagger Codegen
    swaggerCodegen 'io.swagger.codegen.v3:swagger-codegen-cli:3.0.21'
}

// Manifold GraphQL: Configure.
tasks.withType(JavaCompile) {
    options.compilerArgs += ['-Xplugin:Manifold']
}
tasks.compileJava {
    // Add build/resources/main to javac's classpath.
    classpath += files(sourceSets.main.output.resourcesDir)
    dependsOn processResources
}
tasks.compileTestJava {
    // Add build/resources/test to test javac's classpath.
    classpath += files(sourceSets.test.output.resourcesDir)
    dependsOn processTestResources
}

// Swagger Codegen: Define output directory for generated code.
def generatedSourceOutputDir = "${buildDir}/generated-source"

// Swagger Codegen: Configure
swaggerSources {
    openapi {
        // OpenAPI spec location.
        inputFile = file('src/main/resources/public/api-spec.yaml')
        code {
            language = 'spring'
            // Generate only models (vs. components, controllers, etc.).
            components = ['models']
            // Config file location.
            configFile = file('swagger-codegen-config.json')
            outputDir = file("${generatedSourceOutputDir}")
        }
    }
}

// Swagger Codegen: Attach the generated directories as a source for the project.
sourceSets.main.java.srcDir "${generatedSourceOutputDir}/src/main/java"
sourceSets.main.resources.srcDir "${generatedSourceOutputDir}/src/main/resources"

// Spotless: Configure.
spotless {
    java {
        googleJavaFormat()
        importOrder 'java', 'javax', 'org', 'com', 'io'
        removeUnusedImports()
    }
}

test {
    useJUnitPlatform()
}

// Define convenience task for rebuilding application.
task golden {
    dependsOn clean
    dependsOn generateSwaggerCode
    dependsOn spotlessJavaApply
    dependsOn test
    tasks.findByName('generateSwaggerCode').mustRunAfter clean
    tasks.findByName('spotlessJavaApply').mustRunAfter generateSwaggerCode
    tasks.findByName('test').mustRunAfter generateSwaggerCode
}
And our final src/main/resources/application.yaml:

server:
  # Serve the application at 8080.
  port: 8080
  servlet:
    # Prefix all routes with /api.
    contextPath: /api

springdoc:
  swagger-ui:
    # Specify the OpenAPI spec location.
    url: /api/api-spec.yaml
Run the Application
Run ./gradlew golden to get a clean build, then start the application.

To see it in action, try the following:

cURL
Request:

curl localhost:8080/api/v1/cities/Minneapolis
Sample response:

{"id":"5037649","name":"Minneapolis","country":"US","coord":{"lon":-93.26,"lat":44.98},"weather":{"summary":{"title":"Clouds","description":"overcast clouds","icon":"04d"},"temperature":{"actual":281.95,"feelsLike":277.75,"min":281.15,"max":282.59},"wind":{"speed":4.6,"deg":360},"clouds":{"all":90,"visibility":10000,"humidity":81},"timestamp":1599581857}}
Swagger UI
http://localhost:8080/api/swagger-ui/index.html?configUrl=/api/v3/api-docs/swagger-config

Sample request & response:


Suggested exercise: Add a similar REST endpoint for the getCityById GraphQL query.

Summary
We started out by creating a basic Spring Boot project. Then we converted the GraphQL response into an OpenAPI spec YAML representation, which we used to generate models via Swagger Codegen. We pulled in Manifold to call the GraphQL service from within our Java code. Finally, we defined the service and controller files.

By writing fewer than 150 lines of Java code (including imports, blank lines, and interfaces), we provided a /cities/{city-name} REST endpoint that submits a GraphQL query and returns the full response.

GET localhost:8080/api/v1/cities/Omaha is now equivalent to submitting the following GraphQL query:

query GetCityByName {
  getCityByName(name: "Omaha") {
    id
    name
    country
    coord {
      lon
      lat
    }
    weather {
      summary {
        title
        description
        icon
      }
      temperature {
        actual
        feelsLike
        min
        max
      }
      wind {
        speed
        deg
      }
      clouds {
        all
        visibility
        humidity
      }
      timestamp
    }
  }
}

While this is not the most common GraphQL use case, perhaps it could help you help your clients deliver value for their customers.

About the Author
