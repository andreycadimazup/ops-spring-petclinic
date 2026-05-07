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

package org.springframework.samples.petclinic.owner;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledInNativeImage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.aot.DisabledInAotMode;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Test class for {@link OwnerController}
 *
 * @author Colin But
 * @author Wick Dynex
 */
@WebMvcTest(OwnerController.class)
@DisabledInNativeImage
@DisabledInAotMode
class OwnerControllerTests {

	private static final int TEST_OWNER_ID = 1;

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private OwnerRepository owners;

	@MockitoBean
	private PetTypeRepository petTypes;

	private PetType catType() {
		PetType cat = new PetType();
		cat.setId(1);
		cat.setName("cat");
		return cat;
	}

	private Owner george() {
		Owner george = new Owner();
		george.setId(TEST_OWNER_ID);
		george.setFirstName("George");
		george.setLastName("Franklin");
		george.setAddress("110 W. Liberty St.");
		george.setCity("Madison");
		george.setTelephone("6085551023");
		Pet max = new Pet();
		PetType dog = new PetType();
		dog.setName("dog");
		max.setType(dog);
		max.setName("Max");
		max.setBirthDate(LocalDate.now());
		george.addPet(max);
		max.setId(1);
		return george;
	}

	@BeforeEach
	void setup() {

		Owner george = george();
		given(this.owners.findByCriteria(eq("Franklin"), isNull(), isNull(), isNull(), any(Pageable.class)))
			.willReturn(new PageImpl<>(List.of(george)));
		given(this.petTypes.findPetTypes()).willReturn(List.of(catType()));

		given(this.owners.findById(TEST_OWNER_ID)).willReturn(Optional.of(george));
		Visit visit = new Visit();
		visit.setDate(LocalDate.now());
		george.getPet("Max").getVisits().add(visit);

	}

	@Test
	void initCreationForm() throws Exception {
		mockMvc.perform(get("/owners/new"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("owner"))
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void processCreationFormSuccess() throws Exception {
		mockMvc
			.perform(post("/owners/new").param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "123 Caramel Street")
				.param("city", "London")
				.param("telephone", "1316761638"))
			.andExpect(status().is3xxRedirection());
	}

	@Test
	void processCreationFormHasErrors() throws Exception {
		mockMvc
			.perform(post("/owners/new").param("firstName", "Joe").param("lastName", "Bloggs").param("city", "London"))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("owner"))
			.andExpect(model().attributeHasFieldErrors("owner", "address"))
			.andExpect(model().attributeHasFieldErrors("owner", "telephone"))
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void initFindForm() throws Exception {
		mockMvc.perform(get("/owners/find"))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("ownerSearch"))
			.andExpect(model().attributeExists("petTypes"))
			.andExpect(view().name("owners/findOwners"));
	}

	@Test
	void processFindFormSuccess() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george(), new Owner()));
		when(this.owners.findByCriteria(isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("totalItems", 2L))
			.andExpect(view().name("owners/ownersList"));
	}

	@Test
	void processFindFormByLastName() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george()));
		when(this.owners.findByCriteria(eq("Franklin"), isNull(), isNull(), isNull(), any(Pageable.class)))
			.thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1").param("lastName", "Franklin"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("listOwners", hasItem(hasProperty("lastName", is("Franklin")))))
			.andExpect(view().name("owners/ownersList"));
	}

	@Test
	void processFindFormWithMultipleFilters() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george()));
		when(this.owners.findByCriteria(eq("Franklin"), eq("Madison"), eq("608555"), eq(1), any(Pageable.class)))
			.thenReturn(tasks);
		mockMvc
			.perform(get("/owners?page=1").param("lastName", " Franklin ")
				.param("city", "Madison")
				.param("telephone", "608555")
				.param("petTypeId", "1"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("hasActiveFilters", true))
			.andExpect(view().name("owners/ownersList"));

		verify(this.owners).findByCriteria(eq("Franklin"), eq("Madison"), eq("608555"), eq(1), any(Pageable.class));
	}

	@Test
	void processFindFormWithBlankFieldsNormalizesFilters() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of(george()));
		when(this.owners.findByCriteria(isNull(), isNull(), isNull(), isNull(), any(Pageable.class))).thenReturn(tasks);

		mockMvc.perform(get("/owners?page=1").param("lastName", " ").param("city", "").param("telephone", " "))
			.andExpect(status().isOk())
			.andExpect(model().attribute("hasActiveFilters", false))
			.andExpect(view().name("owners/ownersList"));

		verify(this.owners).findByCriteria(isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
	}

	@Test
	void processFindFormNoOwnersFound() throws Exception {
		Page<Owner> tasks = new PageImpl<>(List.of());
		when(this.owners.findByCriteria(eq("Unknown Surname"), isNull(), isNull(), isNull(), any(Pageable.class)))
			.thenReturn(tasks);
		mockMvc.perform(get("/owners?page=1").param("lastName", "Unknown Surname"))
			.andExpect(status().isOk())
			.andExpect(model().attribute("listOwners", hasSize(0)))
			.andExpect(content().string(containsString("No owners found for the provided filters.")))
			.andExpect(view().name("owners/ownersList"));

	}

	@Test
	void processFindFormPaginationPreservesFilters() throws Exception {
		Pageable firstPage = PageRequest.of(0, 5);
		Page<Owner> tasks = new PageImpl<>(List.of(george(), george(), george(), george(), george()), firstPage, 6);
		when(this.owners.findByCriteria(eq("Franklin"), eq("Madison"), eq("608555"), eq(1), any(Pageable.class)))
			.thenReturn(tasks);

		mockMvc
			.perform(get("/owners?page=1").param("lastName", "Franklin")
				.param("city", "Madison")
				.param("telephone", "608555")
				.param("petTypeId", "1"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("lastName=Franklin")))
			.andExpect(content().string(containsString("city=Madison")))
			.andExpect(content().string(containsString("telephone=608555")))
			.andExpect(content().string(containsString("petTypeId=1")))
			.andExpect(view().name("owners/ownersList"));
	}

	@Test
	void initUpdateOwnerForm() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}/edit", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(model().attributeExists("owner"))
			.andExpect(model().attribute("owner", hasProperty("lastName", is("Franklin"))))
			.andExpect(model().attribute("owner", hasProperty("firstName", is("George"))))
			.andExpect(model().attribute("owner", hasProperty("address", is("110 W. Liberty St."))))
			.andExpect(model().attribute("owner", hasProperty("city", is("Madison"))))
			.andExpect(model().attribute("owner", hasProperty("telephone", is("6085551023"))))
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void processUpdateOwnerFormSuccess() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/edit", TEST_OWNER_ID).param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "123 Caramel Street")
				.param("city", "London")
				.param("telephone", "1616291589"))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void processUpdateOwnerFormUnchangedSuccess() throws Exception {
		mockMvc.perform(post("/owners/{ownerId}/edit", TEST_OWNER_ID))
			.andExpect(status().is3xxRedirection())
			.andExpect(view().name("redirect:/owners/{ownerId}"));
	}

	@Test
	void processUpdateOwnerFormHasErrors() throws Exception {
		mockMvc
			.perform(post("/owners/{ownerId}/edit", TEST_OWNER_ID).param("firstName", "Joe")
				.param("lastName", "Bloggs")
				.param("address", "")
				.param("telephone", ""))
			.andExpect(status().isOk())
			.andExpect(model().attributeHasErrors("owner"))
			.andExpect(model().attributeHasFieldErrors("owner", "address"))
			.andExpect(model().attributeHasFieldErrors("owner", "telephone"))
			.andExpect(view().name("owners/createOrUpdateOwnerForm"));
	}

	@Test
	void showOwner() throws Exception {
		mockMvc.perform(get("/owners/{ownerId}", TEST_OWNER_ID))
			.andExpect(status().isOk())
			.andExpect(model().attribute("owner", hasProperty("lastName", is("Franklin"))))
			.andExpect(model().attribute("owner", hasProperty("firstName", is("George"))))
			.andExpect(model().attribute("owner", hasProperty("address", is("110 W. Liberty St."))))
			.andExpect(model().attribute("owner", hasProperty("city", is("Madison"))))
			.andExpect(model().attribute("owner", hasProperty("telephone", is("6085551023"))))
			.andExpect(model().attribute("owner", hasProperty("pets", not(empty()))))
			.andExpect(model().attribute("owner",
					hasProperty("pets", hasItem(hasProperty("visits", hasSize(greaterThan(0)))))))
			.andExpect(view().name("owners/ownerDetails"));
	}

	@Test
	void processUpdateOwnerFormWithIdMismatch() throws Exception {
		int pathOwnerId = 1;

		Owner owner = new Owner();
		owner.setId(2);
		owner.setFirstName("John");
		owner.setLastName("Doe");
		owner.setAddress("Center Street");
		owner.setCity("New York");
		owner.setTelephone("0123456789");

		when(owners.findById(pathOwnerId)).thenReturn(Optional.of(owner));

		mockMvc.perform(MockMvcRequestBuilders.post("/owners/{ownerId}/edit", pathOwnerId).flashAttr("owner", owner))
			.andExpect(status().is3xxRedirection())
			.andExpect(redirectedUrl("/owners/" + pathOwnerId + "/edit"))
			.andExpect(flash().attributeExists("error"));
	}

}
