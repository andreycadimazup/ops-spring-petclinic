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

import org.springframework.util.StringUtils;

class OwnerSearchCriteria {

	private String lastName;

	private String city;

	private String telephone;

	private Integer petTypeId;

	String normalizedLastName() {
		return normalize(this.lastName);
	}

	String normalizedCity() {
		return normalize(this.city);
	}

	String normalizedTelephone() {
		return normalize(this.telephone);
	}

	boolean hasFilters() {
		return normalizedLastName() != null || normalizedCity() != null || normalizedTelephone() != null
				|| this.petTypeId != null;
	}

	private String normalize(String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

	public String getLastName() {
		return this.lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getCity() {
		return this.city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getTelephone() {
		return this.telephone;
	}

	public void setTelephone(String telephone) {
		this.telephone = telephone;
	}

	public Integer getPetTypeId() {
		return this.petTypeId;
	}

	public void setPetTypeId(Integer petTypeId) {
		this.petTypeId = petTypeId;
	}

}
