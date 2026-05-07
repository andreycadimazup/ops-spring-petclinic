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

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@RestController
@RequestMapping("/api/v1")
@Validated
class OwnerRestController {

	private final RestOwnerService owners;

	private final RestOwnerMapper mapper;

	OwnerRestController(RestOwnerService owners, RestOwnerMapper mapper) {
		this.owners = owners;
		this.mapper = mapper;
	}

	@GetMapping("/owners")
	PageResponse<OwnerSummaryResponse> findOwners(@RequestParam(required = false) String lastName,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		Page<Owner> ownersPage = this.owners.findOwners(lastName, PageRequest.of(page, size));
		return new PageResponse<>(ownersPage.map(this.mapper::toOwnerSummary).toList(), ownersPage.getNumber(),
				ownersPage.getSize(), ownersPage.getTotalElements(), ownersPage.getTotalPages());
	}

	@GetMapping("/owners/{ownerId}")
	OwnerResponse findOwner(@PathVariable int ownerId) {
		return this.mapper.toOwnerResponse(this.owners.findOwner(ownerId));
	}

	@PostMapping("/owners")
	ResponseEntity<OwnerResponse> createOwner(@Valid @RequestBody OwnerRequest request) {
		Owner owner = this.owners.createOwner(request);
		return ResponseEntity.created(location("/{id}", owner.getId())).body(this.mapper.toOwnerResponse(owner));
	}

	@PutMapping("/owners/{ownerId}")
	OwnerResponse updateOwner(@PathVariable int ownerId, @Valid @RequestBody OwnerRequest request) {
		return this.mapper.toOwnerResponse(this.owners.updateOwner(ownerId, request));
	}

	@GetMapping("/pet-types")
	List<PetTypeResponse> findPetTypes() {
		return this.owners.findPetTypes().stream().map(this.mapper::toPetTypeResponse).toList();
	}

	@GetMapping("/owners/{ownerId}/pets")
	List<PetResponse> findPets(@PathVariable int ownerId) {
		Owner owner = this.owners.findOwner(ownerId);
		return this.mapper.toPetResponses(owner);
	}

	@GetMapping("/owners/{ownerId}/pets/{petId}")
	PetResponse findPet(@PathVariable int ownerId, @PathVariable int petId) {
		return this.mapper.toPetResponse(ownerId, this.owners.findPet(ownerId, petId));
	}

	@PostMapping("/owners/{ownerId}/pets")
	ResponseEntity<PetResponse> createPet(@PathVariable int ownerId, @Valid @RequestBody PetRequest request) {
		Pet pet = this.owners.createPet(ownerId, request);
		return ResponseEntity.created(location("/{id}", pet.getId())).body(this.mapper.toPetResponse(ownerId, pet));
	}

	@PutMapping("/owners/{ownerId}/pets/{petId}")
	PetResponse updatePet(@PathVariable int ownerId, @PathVariable int petId, @Valid @RequestBody PetRequest request) {
		return this.mapper.toPetResponse(ownerId, this.owners.updatePet(ownerId, petId, request));
	}

	@GetMapping("/owners/{ownerId}/pets/{petId}/visits")
	List<VisitResponse> findVisits(@PathVariable int ownerId, @PathVariable int petId) {
		return this.owners.findVisits(ownerId, petId)
			.stream()
			.map((visit) -> this.mapper.toVisitResponse(petId, visit))
			.toList();
	}

	@GetMapping("/owners/{ownerId}/pets/{petId}/visits/{visitId}")
	VisitResponse findVisit(@PathVariable int ownerId, @PathVariable int petId, @PathVariable int visitId) {
		return this.mapper.toVisitResponse(petId, this.owners.findVisit(ownerId, petId, visitId));
	}

	@PostMapping("/owners/{ownerId}/pets/{petId}/visits")
	ResponseEntity<VisitResponse> createVisit(@PathVariable int ownerId, @PathVariable int petId,
			@Valid @RequestBody VisitRequest request) {
		Visit visit = this.owners.createVisit(ownerId, petId, request);
		return ResponseEntity.created(location("/{id}", visit.getId())).body(this.mapper.toVisitResponse(petId, visit));
	}

	private URI location(String path, Object... uriVariables) {
		return ServletUriComponentsBuilder.fromCurrentRequest().path(path).buildAndExpand(uriVariables).toUri();
	}

}
