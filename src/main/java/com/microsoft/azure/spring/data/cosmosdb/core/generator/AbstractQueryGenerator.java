/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */
package com.microsoft.azure.spring.data.cosmosdb.core.generator;

import com.microsoft.azure.spring.data.cosmosdb.core.convert.MappingDocumentDbConverter;
import com.microsoft.azure.spring.data.cosmosdb.core.query.Criteria;
import com.microsoft.azure.spring.data.cosmosdb.core.query.CriteriaType;
import com.microsoft.azure.spring.data.cosmosdb.core.query.DocumentQuery;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.javatuples.Pair;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
public abstract class AbstractQueryGenerator {

    private String generateUnaryQuery(@NonNull Criteria criteria, @NonNull List<Pair<String, Object>> parameters) {
        Assert.isTrue(criteria.getSubjectValues().size() == 1, "Unary criteria should have only one subject value");
        Assert.isTrue(CriteriaType.isUnary(criteria.getType()), "Criteria type should be unary operation");

        final String subject = criteria.getSubject();
        final Object subjectValue = MappingDocumentDbConverter.toDocumentDBValue(criteria.getSubjectValues().get(0));

        parameters.add(Pair.with(subject, subjectValue));

        return String.format("r.%s%s@%s", subject, criteria.getType().getSqlKeyword(), subject);
    }

    private String generateBinaryQuery(@NonNull String left, @NonNull String right, CriteriaType type) {
        Assert.isTrue(CriteriaType.isBinary(type), "Criteria type should be binary operation");

        return String.join(" ", left, type.getSqlKeyword(), right);
    }

    private String generateQueryBody(@NonNull Criteria criteria, @NonNull List<Pair<String, Object>> parameters) {
        final CriteriaType type = criteria.getType();

        switch (type) {
            case IS_EQUAL:
            case BEFORE:
            case AFTER:
            case GREATER_THAN:
                return this.generateUnaryQuery(criteria, parameters);
            case AND:
            case OR:
                Assert.isTrue(criteria.getSubCriteria().size() == 2, "criteria should have two SubCriteria");

                final String left = generateQueryBody(criteria.getSubCriteria().get(0), parameters);
                final String right = generateQueryBody(criteria.getSubCriteria().get(1), parameters);

                return generateBinaryQuery(left, right, type);
            default:
                throw new UnsupportedOperationException("unsupported Criteria type" + type);
        }
    }

    /**
     * Generate a query body for interface QuerySpecGenerator.
     * The query body compose of Sql query String and its' parameters.
     * The parameters organized as a list of Pair, for each pair compose parameter name and value.
     *
     * @param query the representation for query method.
     * @return A pair tuple compose of Sql query.
     */
    @NonNull
    protected Pair<String, List<Pair<String, Object>>> generateQueryBody(@NonNull DocumentQuery query) {
        final List<Pair<String, Object>> parameters = new ArrayList<>();
        final String queryString = this.generateQueryBody(query.getCriteria(), parameters);

        return Pair.with(queryString, parameters);
    }
}
