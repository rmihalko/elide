/*
 * Copyright 2019, Yahoo Inc.
 * Licensed under the Apache License, Version 2.0
 * See LICENSE file in project root for terms.
 */

package com.yahoo.elide.datastores.search;

import static com.jayway.restassured.RestAssured.given;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attr;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.attributes;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.data;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.id;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.resource;
import static com.yahoo.elide.contrib.testhelpers.jsonapi.JsonApiDSL.type;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;

import com.yahoo.elide.core.HttpStatus;
import com.yahoo.elide.initialization.AbstractApiResourceInitializer;

import com.jayway.restassured.internal.mapper.ObjectMapperType;
import org.testng.annotations.Test;

import java.util.Arrays;

public class SearchDataStoreITTest extends AbstractApiResourceInitializer {

    public SearchDataStoreITTest() {
        super(DependencyBinder.class);
    }

    @Test
    public void getEscapedItem() {
        given()
            .contentType("application/vnd.api+json")
            .when()
            .get("/item?filter[item]=name==*-Luc*")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", equalTo(Arrays.asList("6")));
    }

    @Test
    public void testObjectIndexing() {
       /* Add a new item */
       given()
           .contentType("application/vnd.api+json")
           .body(
                   data(
                       resource(
                          type("item"),
                          id(1000),
                          attributes(
                                  attr("name", "Another Drum"),
                                  attr("description", "Onyx Timpani Drum")
                          )
                       )
                   ), ObjectMapperType.GSON)
           .when()
           .post("/item")
           .then()
           .statusCode(org.apache.http.HttpStatus.SC_CREATED);

        /* This query hits the index */
        given()
            .contentType("application/vnd.api+json")
            .when()
            .get("/item?filter[item]=name==*DrU*")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "3", "1000"));

        /* This query hits the DB */
        given()
            .contentType("application/vnd.api+json")
            .when()
            .get("/item")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "2", "3", "4", "5", "6", "7", "1000"));

        /* Delete the newly added item */
        given()
            .contentType("application/vnd.api+json")
            .when()
            .delete("/item/1000")
            .then()
            .statusCode(HttpStatus.SC_NO_CONTENT);

        /* This query hits the index */
        given()
            .contentType("application/vnd.api+json")
            .when()
            .get("/item?filter[item]=name==*DrU*")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "3"));

        /* This query hits the DB */
        given()
            .contentType("application/vnd.api+json")
            .when()
            .get("/item")
            .then()
            .statusCode(HttpStatus.SC_OK)
            .body("data.id", containsInAnyOrder("1", "2", "3", "4", "5", "6", "7"));
    }
}
