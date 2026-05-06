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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import jakarta.persistence.EntityManagerFactory;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureTestDatabase(replace = Replace.NONE)
class VetRepositoryTests {

	@Autowired
	private VetRepository vets;

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private Statistics statistics;

	@BeforeEach
	void setup() {
		this.statistics = this.entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
		this.statistics.clear();
	}

	@Test
	void shouldFetchSpecialtiesWhenFindingAllVets() {
		Collection<Vet> foundVets = this.vets.findAll();

		assertThat(foundVets).hasSize(6);
		Vet douglas = foundVets.stream().filter(vet -> vet.getId().equals(3)).findFirst().orElseThrow();
		long statementsAfterQuery = this.statistics.getPrepareStatementCount();

		assertThat(douglas.getSpecialties()).extracting(Specialty::getName).containsExactly("dentistry", "surgery");
		assertThat(this.statistics.getPrepareStatementCount()).isEqualTo(statementsAfterQuery);
		assertThat(statementsAfterQuery).isEqualTo(1);
	}

	@Test
	void shouldFetchSpecialtiesForPagedVetsWithoutNPlusOneQueries() {
		Page<Vet> page = this.vets.findAll(PageRequest.of(0, 5, Sort.by("id")));

		assertThat(page.getContent()).extracting(Vet::getId).containsExactly(1, 2, 3, 4, 5);
		Vet douglas = page.getContent().stream().filter(vet -> vet.getId().equals(3)).findFirst().orElseThrow();
		long statementsAfterQuery = this.statistics.getPrepareStatementCount();

		assertThat(douglas.getSpecialties()).extracting(Specialty::getName).containsExactly("dentistry", "surgery");
		page.getContent().forEach(Vet::getNrOfSpecialties);
		assertThat(this.statistics.getPrepareStatementCount()).isEqualTo(statementsAfterQuery);
		assertThat(statementsAfterQuery).isLessThanOrEqualTo(3);
	}

}
