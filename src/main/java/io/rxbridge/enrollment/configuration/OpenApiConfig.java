package io.rxbridge.enrollment.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class OpenApiConfig {

    private static final String PROBLEM_JSON = "application/problem+json";
    private static final String PROBLEM = "Problem";

    @Bean
    OpenAPI enrollmentServiceApi() {
        return new OpenAPI()
            .info(
                new Info()
                    .title("RxBridge Enrollment Service API")
                    .version("1.0")
                    .description("Enrollments of patients")
            ).addServersItem(
                new Server().url("http://localhost:8080")
            );
    }

    @Bean
    OpenApiCustomizer problemResponse() {
        return openApi -> {
            openApi.getComponents().addSchemas(PROBLEM, problemSchema());
            openApi.getPaths().values().forEach(
                path -> path.readOperations()
                    .forEach(OpenApiConfig::describeErrors)
            );
        };
    }

    private static void describeErrors(Operation operation) {
        ApiResponses responses = operation.getResponses();
        responses.computeIfAbsent("400", code -> new ApiResponse().description("The request is not valid"));
        responses.computeIfAbsent("500", code -> new ApiResponse().description("Unexpected error"));
        responses.forEach((code, response) -> {
            if (code.startsWith("4") || code.startsWith("5")) {
                response.setContent(new Content().addMediaType(PROBLEM_JSON,
                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM))));
            }
        });
    }

    private static Schema<?> problemSchema() {
        Schema<?> fieldProblem = new ObjectSchema()
            .addProperty("field", new StringSchema())
            .addProperty("message", new StringSchema());

        return new ObjectSchema()
            .description("RFC 9457 problem details")
            .addProperty("title", new StringSchema())
            .addProperty("status", new IntegerSchema())
            .addProperty("detail", new StringSchema())
            .addProperty("type", new StringSchema())
            .addProperty("instance", new StringSchema())
            .addProperty("errors", new ArraySchema().items(fieldProblem));
    }

}
