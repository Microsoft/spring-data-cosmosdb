/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See LICENSE in the project root for
 * license information.
 */

package com.microsoft.azure.spring.data.cosmosdb.repository;

import com.microsoft.azure.spring.data.cosmosdb.common.TestConstants;
import com.microsoft.azure.spring.data.cosmosdb.core.DocumentDbOperations;
import com.microsoft.azure.spring.data.cosmosdb.domain.Address;
import com.microsoft.azure.spring.data.cosmosdb.domain.Person;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.DocumentDbEntityInformation;
import com.microsoft.azure.spring.data.cosmosdb.repository.support.SimpleDocumentDbRepository;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import rx.Observable;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SimpleDocumentDbRepositoryUnitTest {
    private static final Person TEST_PERSON =
            new Person(TestConstants.ID_1, TestConstants.FIRST_NAME, TestConstants.LAST_NAME,
                    TestConstants.HOBBIES, TestConstants.ADDRESSES);

    private static final String PARTITION_VALUE_REQUIRED_MSG =
            "PartitionKey value must be supplied for this operation.";

    SimpleDocumentDbRepository<Person, String> repository;
    @Mock
    DocumentDbOperations dbOperations;
    @Mock
    DocumentDbEntityInformation<Person, String> entityInformation;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        when(entityInformation.getJavaType()).thenReturn(Person.class);
        when(entityInformation.getCollectionName()).thenReturn(Person.class.getSimpleName());
        when(dbOperations.findAll(anyString(), any(), any())).thenReturn(Arrays.asList(TEST_PERSON));
        when(dbOperations.findAllAsync(anyString(), any(), any()))
                .thenReturn(Observable.from(Arrays.asList(TEST_PERSON)));
        repository = new SimpleDocumentDbRepository<>(entityInformation, dbOperations);
    }

    @Test
    public void testSave() {
        repository.save(TEST_PERSON);

        final List<Person> result = Lists.newArrayList(repository.findAll());
        assertEquals(1, result.size());
        assertEquals(TEST_PERSON, result.get(0));
    }

    @Test
    public void testFindOne() {
        when(dbOperations.findById(anyString(), any(), any(), any())).thenReturn(Optional.of(TEST_PERSON));

        repository.save(TEST_PERSON);

        final Optional<Person> optional = repository.findById(TEST_PERSON.getId());
        assertTrue(optional.isPresent());
        assertEquals(TEST_PERSON, optional.get());
    }

    @Test
    public void testFindOneExceptionForPartitioned() {
        expectedException.expect(UnsupportedOperationException.class);
        expectedException.expectMessage(PARTITION_VALUE_REQUIRED_MSG);

        repository.save(TEST_PERSON);

        when(dbOperations.findById(anyString(), any(), any(), any()))
                .thenThrow(new UnsupportedOperationException(PARTITION_VALUE_REQUIRED_MSG));

        final Person result = repository.findById(TEST_PERSON.getId()).get();
    }

    @Test
    public void testUpdate() {
        final List<Address> updatedAddress =
                Arrays.asList(new Address(TestConstants.POSTAL_CODE, TestConstants.UPDATED_CITY,
                        TestConstants.UPDATED_STREET));
        final Person updatedPerson =
                new Person(TEST_PERSON.getId(), TestConstants.UPDATED_FIRST_NAME, TestConstants.UPDATED_LAST_NAME,
                        TestConstants.UPDATED_HOBBIES, updatedAddress);
        repository.save(updatedPerson);

        when(dbOperations.findById(anyString(), any(), any(), any())).thenReturn(Optional.of(updatedPerson));

        final Optional<Person> optional = repository.findById(TEST_PERSON.getId());

        assertTrue(optional.isPresent());
        assertEquals(updatedPerson, optional.get());
    }

    @Test
    public void findAllAsync() {
        final Observable<Person> personObservable = repository.findAllAsync();

        assertEquals(TEST_PERSON, personObservable.toBlocking().single());
    }
}
