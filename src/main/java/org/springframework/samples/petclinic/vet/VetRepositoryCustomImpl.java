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
package org.springframework.samples.petclinic.vet;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class VetRepositoryCustomImpl implements VetRepositoryCustom {

	private final EntityManager entityManager;

	VetRepositoryCustomImpl(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	@Override
	public Collection<Vet> findAllWithSpecialties() {
		return this.entityManager
			.createQuery("select distinct vet from Vet vet left join fetch vet.specialties order by vet.id", Vet.class)
			.getResultList();
	}

	@Override
	public Page<Vet> findAllWithSpecialties(Pageable pageable) {
		Long total = this.entityManager.createQuery("select count(vet) from Vet vet", Long.class).getSingleResult();
		if (total == 0) {
			return new PageImpl<>(List.of(), pageable, total);
		}

		List<Integer> vetIds = findVetIds(pageable);
		if (vetIds.isEmpty()) {
			return new PageImpl<>(List.of(), pageable, total);
		}

		List<Vet> vets = this.entityManager
			.createQuery("select distinct vet from Vet vet left join fetch vet.specialties where vet.id in :ids",
					Vet.class)
			.setParameter("ids", vetIds)
			.getResultList();
		Map<Integer, Vet> vetsById = vets.stream().collect(Collectors.toMap(Vet::getId, Function.identity()));
		List<Vet> sortedVets = vetIds.stream().map(vetsById::get).toList();

		return new PageImpl<>(sortedVets, pageable, total);
	}

	private List<Integer> findVetIds(Pageable pageable) {
		TypedQuery<Integer> query = this.entityManager
			.createQuery("select vet.id from Vet vet" + orderBy(pageable.getSort()), Integer.class);

		if (pageable.isPaged()) {
			query.setFirstResult((int) pageable.getOffset());
			query.setMaxResults(pageable.getPageSize());
		}

		return query.getResultList();
	}

	private String orderBy(Sort sort) {
		if (sort.isUnsorted()) {
			return " order by vet.id";
		}

		String order = sort.stream().map(this::orderBy).collect(Collectors.joining(", "));

		return " order by " + order;
	}

	private String orderBy(Sort.Order order) {
		return "vet." + sortProperty(order.getProperty()) + " " + order.getDirection().name();
	}

	private String sortProperty(String property) {
		return switch (property) {
			case "id", "firstName", "lastName" -> property;
			default -> throw new IllegalArgumentException("Unsupported sort property: " + property);
		};
	}

}
