package com.trafficparrot.demo.product.details;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.ErrorType;

public class GraphQLErrorException extends RuntimeException {
    public GraphQLErrorException(String message) {
        super(message);
    }

    public GraphQLError toGraphQLError(DataFetchingEnvironment environment) {
        return GraphqlErrorBuilder.newError()
                .errorType(ErrorType.NOT_FOUND)
                .message(getMessage())
                .path(environment.getExecutionStepInfo().getPath())
                .location(environment.getField().getSourceLocation())
                .build();
    }
}
