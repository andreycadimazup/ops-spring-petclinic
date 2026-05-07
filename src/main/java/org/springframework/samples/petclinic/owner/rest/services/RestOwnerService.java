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

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.owner.Owner;
import org.springframework.samples.petclinic.owner.OwnerRepository;
import org.springframework.samples.petclinic.owner.Pet;
import org.springframework.samples.petclinic.owner.PetType;
import org.springframework.samples.petclinic.owner.PetTypeRepository;
import org.springframework.samples.petclinic.owner.Visit;
import org.springframework.samples.petclinic.owner.rest.exceptions.ApiValidationException;
import org.springframework.samples.petclinic.owner.rest.exceptions.BusinessConflictException;
import org.springframework.samples.petclinic.owner.rest.exceptions.ResourceNotFoundException;
import org.springframework.samples.petclinic.owner.rest.requests.OwnerRequest;
import org.springframework.samples.petclinic.owner.rest.requests.PetRequest;
import org.springframework.samples.petclinic.owner.rest.requests.VisitRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RestOwnerService {

	private final OwnerRepository owners;

	private final PetTypeRepository petTypes;

	RestOwnerService(OwnerRepository owners, PetTypeRepository petTypes) {
		this.owners = owners;
		this.petTypes = petTypes;
	}

	@Transactional(readOnly = true)
	public Page<Owner> findOwners(String lastName, Pageable pageable) {
		return this.owners.findByLastNameStartingWith(lastName == null ? "" : lastName, pageable);
	}

	@Transactional(readOnly = true)
	public Owner findOwner(int ownerId) {
		return findExistingOwnerWithPetsAndVisits(ownerId);
	}

	@Transactional
	public Owner createOwner(OwnerRequest request) {
		Owner owner = new Owner();
		applyOwnerRequest(owner, request);
		return this.owners.save(owner);
	}

	@Transactional
	public Owner updateOwner(int ownerId, OwnerRequest request) {
		Owner owner = findExistingOwner(ownerId);
		applyOwnerRequest(owner, request);
		this.owners.save(owner);
		return findExistingOwnerWithPetsAndVisits(ownerId);
	}

	@Transactional(readOnly = true)
	public List<PetType> findPetTypes() {
		return this.petTypes.findPetTypes();
	}

	@Transactional(readOnly = true)
	public List<Pet> findPets(int ownerId) {
		return findExistingOwnerWithPetsAndVisits(ownerId).getPets();
	}

	@Transactional(readOnly = true)
	public Pet findPet(int ownerId, int petId) {
		Owner owner = findExistingOwnerWithPetsAndVisits(ownerId);
		return findExistingPet(owner, petId);
	}

	@Transactional
	public Pet createPet(int ownerId, PetRequest request) {
		Owner owner = findExistingOwnerWithPetsAndTypes(ownerId);
		PetType type = findExistingPetType(request.typeId());
		assertPetNameAvailable(owner, request.name(), null);

		Pet pet = new Pet();
		applyPetRequest(pet, request, type);
		owner.addPet(pet);
		Owner savedOwner = this.owners.saveAndFlush(owner);
		return savedOwner.getPet(request.name());
	}

	@Transactional
	public Pet updatePet(int ownerId, int petId, PetRequest request) {
		Owner owner = findExistingOwnerWithPetsAndVisits(ownerId);
		Pet pet = findExistingPet(owner, petId);
		PetType type = findExistingPetType(request.typeId());
		assertPetNameAvailable(owner, request.name(), petId);

		applyPetRequest(pet, request, type);
		this.owners.save(owner);
		return pet;
	}

	@Transactional(readOnly = true)
	public List<Visit> findVisits(int ownerId, int petId) {
		return List.copyOf(findPet(ownerId, petId).getVisits());
	}

	@Transactional(readOnly = true)
	public Visit findVisit(int ownerId, int petId, int visitId) {
		Pet pet = findPet(ownerId, petId);
		return findExistingVisit(pet, visitId);
	}

	@Transactional
	public Visit createVisit(int ownerId, int petId, VisitRequest request) {
		Owner owner = findExistingOwnerWithPetsAndVisits(ownerId);
		Pet pet = findExistingPet(owner, petId);

		Visit visit = new Visit();
		visit.setDate(request.date() == null ? LocalDate.now() : request.date());
		visit.setDescription(request.description());
		owner.addVisit(pet.getId(), visit);
		Owner savedOwner = this.owners.saveAndFlush(owner);
		Pet savedPet = findExistingPet(savedOwner, petId);
		return savedPet.getVisits()
			.stream()
			.filter((savedVisit) -> savedVisit.getDate().equals(visit.getDate())
					&& savedVisit.getDescription().equals(visit.getDescription()))
			.reduce((first, second) -> second)
			.orElse(visit);
	}

	private Owner findExistingOwner(int ownerId) {
		return this.owners.findById(ownerId)
			.orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " was not found"));
	}

	private Owner findExistingOwnerWithPetsAndTypes(int ownerId) {
		return this.owners.findWithPetsAndTypesById(ownerId)
			.orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " was not found"));
	}

	private Owner findExistingOwnerWithPetsAndVisits(int ownerId) {
		return this.owners.findWithPetsAndVisitsById(ownerId)
			.orElseThrow(() -> new ResourceNotFoundException("Owner " + ownerId + " was not found"));
	}

	private Pet findExistingPet(Owner owner, int petId) {
		Pet pet = owner.getPet(petId);
		if (pet == null) {
			throw new ResourceNotFoundException("Pet " + petId + " was not found for owner " + owner.getId());
		}
		return pet;
	}

	private Visit findExistingVisit(Pet pet, int visitId) {
		return pet.getVisits()
			.stream()
			.filter((visit) -> Objects.equals(visit.getId(), visitId))
			.findFirst()
			.orElseThrow(
					() -> new ResourceNotFoundException("Visit " + visitId + " was not found for pet " + pet.getId()));
	}

	private PetType findExistingPetType(Integer typeId) {
		return this.petTypes.findById(typeId)
			.orElseThrow(() -> new ApiValidationException("typeId", "must reference an existing pet type"));
	}

	private void assertPetNameAvailable(Owner owner, String petName, Integer currentPetId) {
		Pet existingPet = owner.getPet(petName, false);
		if (existingPet != null && !Objects.equals(existingPet.getId(), currentPetId)) {
			throw new BusinessConflictException("Pet name already exists for this owner");
		}
	}

	private void applyOwnerRequest(Owner owner, OwnerRequest request) {
		owner.setFirstName(request.firstName());
		owner.setLastName(request.lastName());
		owner.setAddress(request.address());
		owner.setCity(request.city());
		owner.setTelephone(request.telephone());
	}

	private void applyPetRequest(Pet pet, PetRequest request, PetType type) {
		pet.setName(request.name());
		pet.setBirthDate(request.birthDate());
		pet.setType(type);
	}

}
