/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.repository;

import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.PagingAndSortingRepository;
import rx.Observable;

import java.io.Serializable;

@NoRepositoryBean
public interface DocumentDbRepository<T, ID extends Serializable> extends PagingAndSortingRepository<T, ID> {

    <S extends T> Observable<S> saveAsync(S domain);

    Observable<Object> deleteByIdAsync(ID id);
}

