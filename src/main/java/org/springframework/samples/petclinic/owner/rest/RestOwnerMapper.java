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
package org.springframework.samples.petclinic.owner.rest;

import java.util.List;

import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.stereotype.Component;

@Component
class RestOwnerMapper {

	OwnerSummaryResponse toOwnerSummary(Owner owner) {
		return new OwnerSummaryResponse(owner.getId(), owner.getFirstName(), owner.getLastName(), owner.getAddress(),
				owner.getCity(), owner.getTelephone());
	}

	OwnerResponse toOwnerResponse(Owner owner) {
		return new OwnerResponse(owner.getId(), owner.getFirstName(), owner.getLastName(), owner.getAddress(),
				owner.getCity(), owner.getTelephone(),
				owner.getPets().stream().map((pet) -> toPetResponse(owner.getId(), pet)).toList());
	}

	PetResponse toPetResponse(Integer ownerId, Pet pet) {
		return new PetResponse(pet.getId(), ownerId, pet.getName(), pet.getBirthDate(),
				toPetTypeResponse(pet.getType()),
				pet.getVisits().stream().map((visit) -> toVisitResponse(pet.getId(), visit)).toList());
	}

	PetTypeResponse toPetTypeResponse(PetType petType) {
		return new PetTypeResponse(petType.getId(), petType.getName());
	}

	VisitResponse toVisitResponse(Integer petId, Visit visit) {
		return new VisitResponse(visit.getId(), petId, visit.getDate(), visit.getDescription());
	}

	List<PetResponse> toPetResponses(Owner owner) {
		return owner.getPets().stream().map((pet) -> toPetResponse(owner.getId(), pet)).toList();
	}

}
