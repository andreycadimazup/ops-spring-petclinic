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
package org.springframework.samples.petclinic.owner.rest.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.rest.requests.OwnerRequest;
import org.springframework.samples.petclinic.owner.rest.requests.PetRequest;
import org.springframework.samples.petclinic.owner.rest.requests.VisitRequest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.docker.compose.enabled=false")
@Transactional
class RestOwnerServiceTests {

	@Autowired
	private RestOwnerService owners;

	@Test
	void shouldCreateOwner() {
		Owner owner = this.owners
			.createOwner(new OwnerRequest(null, "Sam", "Schultz", "4 Evans Street", "Wollongong", "4444444444", null));

		assertThat(owner.getId()).isNotNull();
		assertThat(owner.getLastName()).isEqualTo("Schultz");
		assertThat(this.owners.findOwner(owner.getId()).getTelephone()).isEqualTo("4444444444");
	}

	@Test
	void shouldCreateAndUpdatePet() {
		Pet pet = this.owners.createPet(1, new PetRequest(null, null, null, "Bowser", LocalDate.of(2020, 1, 1), 2));

		assertThat(pet.getId()).isNotNull();
		assertThat(pet.getType().getName()).isEqualTo("dog");

		Pet updated = this.owners.updatePet(1, pet.getId(),
				new PetRequest(null, null, null, "Bowser Jr", LocalDate.of(2020, 1, 2), 3));

		assertThat(updated.getName()).isEqualTo("Bowser Jr");
		assertThat(updated.getBirthDate()).isEqualTo(LocalDate.of(2020, 1, 2));
		assertThat(updated.getType().getName()).isEqualTo("lizard");
	}

	@Test
	void shouldCreateVisitAndPreserveExistingVisits() {
		Pet pet = this.owners.findPet(6, 8);
		int existingVisits = pet.getVisits().size();

		this.owners.createVisit(6, 8, new VisitRequest(null, LocalDate.of(2024, 2, 3), "skin check"));

		Pet updated = this.owners.findPet(6, 8);
		assertThat(updated.getVisits()).hasSize(existingVisits + 1);
		assertThat(updated.getVisits())
			.anySatisfy((visit) -> assertThat(visit.getDescription()).isEqualTo("rabies shot"));
		assertThat(updated.getVisits())
			.anySatisfy((visit) -> assertThat(visit.getDescription()).isEqualTo("skin check"));
	}

}
