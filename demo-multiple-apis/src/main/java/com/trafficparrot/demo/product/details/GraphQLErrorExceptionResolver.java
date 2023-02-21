package com.trafficparrot.demo.product.details;

import graphql.GraphQLError;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.stereotype.Component;

@Component
public class GraphQLErrorExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment environment) {
        if (exception instanceof GraphQLErrorException) {
            GraphQLErrorException graphQLErrorException = (GraphQLErrorException) exception;
            return graphQLErrorException.toGraphQLError(environment);
        } else {
            return super.resolveToSingleError(exception, environment);
        }
    }
}