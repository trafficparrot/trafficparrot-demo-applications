package com.trafficparrot.demo.product.details;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.ErrorType;

public class GraphQLErrorException extends RuntimeException {

    private final ErrorType errorType;

    public GraphQLErrorException(String message, ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    public GraphQLError toGraphQLError(DataFetchingEnvironment environment) {
        return GraphqlErrorBuilder.newError()
                .errorType(errorType)
                .message(getMessage())
                .path(environment.getExecutionStepInfo().getPath())
                .location(environment.getField().getSourceLocation())
                .build();
    }
}
