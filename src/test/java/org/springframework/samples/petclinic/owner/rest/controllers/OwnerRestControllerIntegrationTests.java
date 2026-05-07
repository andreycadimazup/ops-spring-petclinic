/*
 * Copyright 2012-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.owner.rest.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@AutoConfigureMockMvc
@Transactional
class OwnerRestControllerIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldListOwners() throws Exception {
		this.mockMvc.perform(get("/api/v1/owners").param("lastName", "Davis").param("page", "0").param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content", hasSize(2)))
			.andExpect(jsonPath("$.content[0].lastName", is("Davis")))
			.andExpect(jsonPath("$.page", is(0)))
			.andExpect(jsonPath("$.size", is(20)))
			.andExpect(jsonPath("$.totalElements", is(2)));
	}

	@Test
	void shouldFindDetailedOwner() throws Exception {
		this.mockMvc.perform(get("/api/v1/owners/{ownerId}", 6))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(6)))
			.andExpect(jsonPath("$.firstName", is("Jean")))
			.andExpect(jsonPath("$.pets", hasSize(2)))
			.andExpect(jsonPath("$.pets[0].visits", hasSize(2)));
	}

	@Test
	void shouldCreateAndUpdateOwner() throws Exception {
		MvcResult result = this.mockMvc
			.perform(post("/api/v1/owners").contentType(MediaType.APPLICATION_JSON).content("""
					{
					  "firstName": "Alice",
					  "lastName": "Walker",
					  "address": "10 Oak Street",
					  "city": "Madison",
					  "telephone": "6085551111"
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", matchesPattern(".*/api/v1/owners/\\d+")))
			.andExpect(jsonPath("$.id", notNullValue()))
			.andExpect(jsonPath("$.pets", hasSize(0)))
			.andReturn();

		Integer ownerId = JsonPath.read(result.getResponse().getContentAsString(), "$.id");

		this.mockMvc
			.perform(put("/api/v1/owners/{ownerId}", ownerId).contentType(MediaType.APPLICATION_JSON).content("""
					{
					  "firstName": "Alice",
					  "lastName": "Updated",
					  "address": "11 Oak Street",
					  "city": "Sun Prairie",
					  "telephone": "6085552222"
					}
					"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(ownerId)))
			.andExpect(jsonPath("$.lastName", is("Updated")))
			.andExpect(jsonPath("$.telephone", is("6085552222")));
	}

	@Test
	void shouldReturnValidationErrorForInvalidOwner() throws Exception {
		this.mockMvc.perform(post("/api/v1/owners").contentType(MediaType.APPLICATION_JSON).content("""
				{
				  "id": 99,
				  "firstName": "",
				  "lastName": "Walker",
				  "address": "10 Oak Street",
				  "city": "Madison",
				  "telephone": "123",
				  "pets": []
				}
				"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title", is("Validation failed")))
			.andExpect(jsonPath("$.errors.id", notNullValue()))
			.andExpect(jsonPath("$.errors.firstName", notNullValue()))
			.andExpect(jsonPath("$.errors.pets", notNullValue()))
			.andExpect(jsonPath("$.errors.telephone", notNullValue()));
	}

	@Test
	void shouldReturnNotFoundForMissingOwner() throws Exception {
		this.mockMvc.perform(get("/api/v1/owners/{ownerId}", 999))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.title", is("Resource not found")));
	}

	@Test
	void shouldListPetTypesAndCreatePet() throws Exception {
		this.mockMvc.perform(get("/api/v1/pet-types"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(6)))
			.andExpect(jsonPath("$[0].name", is("bird")));

		this.mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets", 1).contentType(MediaType.APPLICATION_JSON).content("""
					{
					  "name": "Milo",
					  "birthDate": "2020-01-02",
					  "typeId": 2
					}
					"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", matchesPattern(".*/api/v1/owners/1/pets/\\d+")))
			.andExpect(jsonPath("$.ownerId", is(1)))
			.andExpect(jsonPath("$.name", is("Milo")))
			.andExpect(jsonPath("$.type.name", is("dog")))
			.andExpect(jsonPath("$.visits", hasSize(0)));
	}

	@Test
	void shouldUpdatePetWithoutDroppingVisits() throws Exception {
		this.mockMvc
			.perform(put("/api/v1/owners/{ownerId}/pets/{petId}", 6, 8).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "name": "Maximo",
						  "birthDate": "2012-09-05",
						  "typeId": 2
						}
						"""))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(8)))
			.andExpect(jsonPath("$.name", is("Maximo")))
			.andExpect(jsonPath("$.type.name", is("dog")))
			.andExpect(jsonPath("$.visits", hasSize(2)));
	}

	@Test
	void shouldReturnPetErrors() throws Exception {
		this.mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets", 6).contentType(MediaType.APPLICATION_JSON).content("""
					{
					  "name": "max",
					  "birthDate": "2020-01-02",
					  "typeId": 1
					}
					"""))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.title", is("Conflict")));

		this.mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets", 6).contentType(MediaType.APPLICATION_JSON).content("""
					{
					  "name": "Future",
					  "birthDate": "2099-01-02",
					  "typeId": 1
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.birthDate", notNullValue()));

		this.mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets", 6).contentType(MediaType.APPLICATION_JSON).content("""
					{
					  "name": "Unknown Type",
					  "birthDate": "2020-01-02",
					  "typeId": 999
					}
					"""))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors.typeId", notNullValue()));
	}

	@Test
	void shouldListFindAndCreateVisit() throws Exception {
		this.mockMvc.perform(get("/api/v1/owners/{ownerId}/pets/{petId}/visits", 6, 8))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$", hasSize(2)))
			.andExpect(jsonPath("$[0].description", is("rabies shot")));

		this.mockMvc.perform(get("/api/v1/owners/{ownerId}/pets/{petId}/visits/{visitId}", 6, 8, 2))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id", is(2)))
			.andExpect(jsonPath("$.petId", is(8)))
			.andExpect(jsonPath("$.description", is("rabies shot")));

		this.mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets/{petId}/visits", 6, 8).contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "description": "annual check"
						}
						"""))
			.andExpect(status().isCreated())
			.andExpect(header().string("Location", matchesPattern(".*/api/v1/owners/6/pets/8/visits/\\d+")))
			.andExpect(jsonPath("$.petId", is(8)))
			.andExpect(jsonPath("$.date", notNullValue()))
			.andExpect(jsonPath("$.description", is("annual check")));
	}

	@Test
	void shouldRunPostOwnerPetVisitThenGetDetailedOwnerFlow() throws Exception {
		MvcResult ownerResult = this.mockMvc
			.perform(post("/api/v1/owners").contentType(MediaType.APPLICATION_JSON).content("""
					{
					  "firstName": "Flow",
					  "lastName": "Owner",
					  "address": "1 Integration Way",
					  "city": "Madison",
					  "telephone": "6085553333"
					}
					"""))
			.andExpect(status().isCreated())
			.andReturn();
		Integer ownerId = JsonPath.read(ownerResult.getResponse().getContentAsString(), "$.id");

		MvcResult petResult = this.mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets", ownerId).contentType(MediaType.APPLICATION_JSON).content("""
					{
					  "name": "FlowPet",
					  "birthDate": "2020-05-01",
					  "typeId": 1
					}
					"""))
			.andExpect(status().isCreated())
			.andReturn();
		Integer petId = JsonPath.read(petResult.getResponse().getContentAsString(), "$.id");

		this.mockMvc
			.perform(post("/api/v1/owners/{ownerId}/pets/{petId}/visits", ownerId, petId)
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{
						  "date": "2024-01-01",
						  "description": "first visit"
						}
						"""))
			.andExpect(status().isCreated());

		this.mockMvc.perform(get("/api/v1/owners/{ownerId}", ownerId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.pets[0].id", is(petId)))
			.andExpect(jsonPath("$.pets[0].visits[0].description", is("first visit")));
	}

}
