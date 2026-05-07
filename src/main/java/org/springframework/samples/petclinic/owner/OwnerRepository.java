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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;


/**
 * Repository class for <code>Owner</code> domain objects. All method names are compliant
 * with Spring Data naming conventions so this interface can easily be extended for Spring
 * Data. See:
 * https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-methods.query-creation
 *
 * @author Ken Krebs
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @author Michael Isvy
 * @author Wick Dynex
 */
public interface OwnerRepository extends JpaRepository<Owner, Integer> {

	/**
	 * Retrieve {@link Owner}s from the data store by last name, returning all owners
	 * whose last name <i>starts</i> with the given name.
	 * @param lastName Value to search for
	 * @return a Collection of matching {@link Owner}s (or an empty Collection if none
	 * found)
	 */
	Page<Owner> findByLastNameStartingWith(String lastName, Pageable pageable);

	@Query(value = """
			select distinct owner
			from Owner owner
			left join owner.pets pet
			where (:lastName is null or lower(owner.lastName) like lower(concat(:lastName, '%')))
			  and (:city is null or lower(owner.city) like lower(concat(:city, '%')))
			  and (:telephone is null or owner.telephone like concat(:telephone, '%'))
			  and (:petTypeId is null or pet.type.id = :petTypeId)
			""", countQuery = """
			select count(distinct owner)
			from Owner owner
			left join owner.pets pet
			where (:lastName is null or lower(owner.lastName) like lower(concat(:lastName, '%')))
			  and (:city is null or lower(owner.city) like lower(concat(:city, '%')))
			  and (:telephone is null or owner.telephone like concat(:telephone, '%'))
			  and (:petTypeId is null or pet.type.id = :petTypeId)
			""")
	Page<Owner> findByCriteria(@Param("lastName") String lastName, @Param("city") String city,
			@Param("telephone") String telephone, @Param("petTypeId") Integer petTypeId, Pageable pageable);

	default Page<Owner> findByLastNameStartingWithWithPets(String lastName, Pageable pageable) {
		Page<Integer> ownerIds = findIdsByLastNameStartingWith(lastName, pageable);
		if (ownerIds.isEmpty()) {
			return Page.empty(pageable);
		}

		Map<Integer, Owner> ownersById = findAllWithPetsByIdIn(ownerIds.getContent()).stream()
			.collect(Collectors.toMap(Owner::getId, Function.identity()));
		List<Owner> owners = ownerIds.getContent().stream().map(ownersById::get).filter(Objects::nonNull).toList();

		return new PageImpl<>(owners, pageable, ownerIds.getTotalElements());
	}

	@Query("select o.id from Owner o where o.lastName like concat(:lastName, '%')")
	Page<Integer> findIdsByLastNameStartingWith(@Param("lastName") String lastName, Pageable pageable);

	@EntityGraph(attributePaths = "pets")
	@Query("select o from Owner o where o.id in :ids")
	List<Owner> findAllWithPetsByIdIn(@Param("ids") Collection<Integer> ids);

	@EntityGraph(attributePaths = { "pets", "pets.type" })
	@Query("select o from Owner o where o.id = :id")
	Optional<Owner> findWithPetsAndTypesById(@Param("id") Integer id);

	@Transactional(readOnly = true)
	default Optional<Owner> findWithPetsAndVisitsById(Integer id) {
		Optional<Owner> owner = findWithPetsAndTypesById(id);
		owner.ifPresent((it) -> it.getPets().forEach((pet) -> pet.getVisits().size()));
		return owner;
	}

	/**
	 * Retrieve an {@link Owner} from the data store by id.
	 * <p>
	 * This method returns an {@link Optional} containing the {@link Owner} if found. If
	 * no {@link Owner} is found with the provided id, it will return an empty
	 * {@link Optional}.
	 * </p>
	 * @param id the id to search for
	 * @return an {@link Optional} containing the {@link Owner} if found, or an empty
	 * {@link Optional} if not found.
	 * @throws IllegalArgumentException if the id is null (assuming null is not a valid
	 * input for id)
	 */
	Optional<Owner> findById(Integer id);

}
