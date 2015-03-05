/**
* Copyright 2014 IHTSDO
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
package com.b2international.snowowlmod.rest.snomed.impl.service;

import com.b2international.snowowl.snomed.datastore.index.refset.SnomedRefSetIndexEntry;
import com.b2international.snowowlmod.rest.snomed.impl.domain.RefSet;

/**
 *Class represent convert a {@link SnomedRefSetIndexEntry} to {@link RefSet}
 */
public class ReferenceSetConverter extends AbstractSnomedComponentConverter<SnomedRefSetIndexEntry, RefSet>  {
	
	@Override
	public RefSet apply(final SnomedRefSetIndexEntry input) {
		
		final RefSet result = new RefSet();
		//result.setActive(input.isActive());
		//result.setEffectiveTime(new Date(input.getEffectiveTimeAsLong()));
		result.setId(input.getId());
		result.setModuleId(input.getModuleId());
		//result.setReleased(input.isReleased());
		result.setTerm(input.getLabel());
		result.setType(input.getType());
		return result;
	}

	

}