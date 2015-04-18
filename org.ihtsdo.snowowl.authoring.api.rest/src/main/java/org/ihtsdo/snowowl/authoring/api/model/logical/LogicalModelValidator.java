package org.ihtsdo.snowowl.authoring.api.model.logical;

import org.ihtsdo.snowowl.api.rest.common.ComponentRefHelper;
import org.ihtsdo.snowowl.authoring.api.Constants;
import org.ihtsdo.snowowl.authoring.api.model.AttributeValidationResult;
import org.ihtsdo.snowowl.authoring.api.model.AuthoringContent;
import org.ihtsdo.snowowl.authoring.api.model.AuthoringContentValidationResult;
import org.ihtsdo.snowowl.authoring.api.services.ContentService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

public class LogicalModelValidator {

	@Autowired
	private ContentService contentService;

	@Autowired
	private ComponentRefHelper componentRefHelper;

	public AuthoringContentValidationResult validate(AuthoringContent content, LogicalModel logicalModel) {
		AuthoringContentValidationResult result = new AuthoringContentValidationResult();
		Map<String, Set<String>> descendantsCache = new HashMap<>();
		validateIsARelationships(content, logicalModel, result, descendantsCache);
		validateAttributes(content, logicalModel, result, descendantsCache);

		return result;
	}

	private void validateIsARelationships(AuthoringContent content, LogicalModel logicalModel, AuthoringContentValidationResult result, Map<String, Set<String>> descendantsCache) {
		List<String> isARelationships = content.getIsARelationships();
		List<IsARestriction> isARestrictions = logicalModel.getIsARestrictions();
		boolean tooManyIsARelationships = isARelationships.size() > isARestrictions.size();
		boolean notEnoughIsARelationships = isARelationships.size() < isARestrictions.size();

		for (int i = 0; i < isARelationships.size(); i++) {
			String message = "";
			if (isARestrictions.size() > i) {
				IsARestriction isARestriction = isARestrictions.get(i);
				String isARelationship = isARelationships.get(i);
				message = validateIsARelationship(isARelationship, isARestriction, descendantsCache);
			}
			result.addIsARelationshipsMessage(message);
		}

		if ((notEnoughIsARelationships || tooManyIsARelationships)) {
			List<String> isARelationshipsMessages = result.getIsARelationshipsMessages();
			String message;
			if (!isARelationshipsMessages.isEmpty()) {
				int lastIndex = isARelationshipsMessages.size() - 1;
				message = isARelationshipsMessages.get(lastIndex);
				isARelationshipsMessages.remove(lastIndex);
			} else {
				message = "";
			}
			if(!message.isEmpty()) {
				message += "\n";
			}
			message += "There are " + (notEnoughIsARelationships ? "less" : "more") + " isA relationships than in the logical model.";
			isARelationshipsMessages.add(message);
		}
	}

	private String validateIsARelationship(String isARelationship, IsARestriction isARestriction, Map<String, Set<String>> descendantsCache) {
		return relatedConceptTest(isARelationship, isARestriction.getRangeRelationType(), isARestriction.getConceptId(), "IsA relation", descendantsCache);
	}

	private void validateAttributes(AuthoringContent content, LogicalModel logicalModel, AuthoringContentValidationResult result, Map<String, Set<String>> descendantsCache) {
		List<LinkedHashMap<String, String>> attributeGroups = content.getAttributeGroups();
		List<List<AttributeRestriction>> attributeRestrictionGroups = logicalModel.getAttributeRestrictionGroups();

		for (int i = 0; i < attributeGroups.size(); i++) {
			LinkedHashMap<String, String> attributeGroup = attributeGroups.get(i);

			Map<String, AttributeRestriction> attributeRestrictionMap = getAttributeRestrictionMap(attributeRestrictionGroups.get(i));
			List<AttributeValidationResult> attributeGroupResults = result.createAttributeGroup();
			List<String> attributeDomains = new ArrayList<>(attributeGroup.keySet());
			for (int groupIndex = 0; groupIndex < attributeGroup.size(); groupIndex++) {
				String attributeDomain = attributeDomains.get(groupIndex);
				String attributeValue = attributeGroup.get(attributeDomain);
				AttributeRestriction attributeRestriction = attributeRestrictionMap.get(attributeDomain);
				if (attributeRestriction != null) {
					validateAttributeValue(attributeValue, attributeRestriction, attributeGroupResults, descendantsCache);
				} else {
					attributeGroupResults.add(new AttributeValidationResult("This attribute was not found in this group (position " + (groupIndex + 1), ")."));
				}
			}

		}
	}

	private void validateAttributeValue(String attributeValue, AttributeRestriction attributeRestriction, List<AttributeValidationResult> attributeGroupResults, Map<String, Set<String>> descendantsCache) {
		String valueMessage = relatedConceptTest(attributeValue, attributeRestriction.getRangeRelationType(), attributeRestriction.getRangeConceptId(), "Attribute value", descendantsCache);
		attributeGroupResults.add(new AttributeValidationResult("", valueMessage));
	}

	private String relatedConceptTest(String conceptToTest, RangeRelationType rangeRelationType, String modelConceptId, String testConceptDescription, Map<String, Set<String>> descendantsCache) {
		String message = "";
		switch (rangeRelationType) {
			case SELF:
				if (!modelConceptId.equals(conceptToTest)) {
					message = testConceptDescription + " must be '" + modelConceptId + "'.";
				}
				break;
			case DESCENDANTS:
				if (!isDescendant(conceptToTest, modelConceptId, descendantsCache)) {
					message = testConceptDescription + " must be a descendant of '" + modelConceptId + "'.";
				}
				break;
			case DESCENDANTS_AND_SELF:
				if (!modelConceptId.equals(conceptToTest) && !isDescendant(conceptToTest, modelConceptId, descendantsCache)) {
					message = testConceptDescription + " must be a descendant of or equal to '" + modelConceptId + "'.";
				}
				break;
		}
		return message;
	}

	private boolean isDescendant(String isARelationship, String conceptId, Map<String, Set<String>> descendantsCache) {
		if (!descendantsCache.containsKey(conceptId)) {
			descendantsCache.put(conceptId, contentService.getDescendantIds(componentRefHelper.createComponentRef(Constants.MAIN, null, conceptId)));
		}
		Set<String> descendantIds = descendantsCache.get(conceptId);
		return descendantIds.contains(isARelationship);
	}

	private Map<String, AttributeRestriction> getAttributeRestrictionMap(List<AttributeRestriction> attributeRestrictions) {
		HashMap<String, AttributeRestriction> map = new HashMap<>();
		for (AttributeRestriction attributeRestriction : attributeRestrictions) {
			map.put(attributeRestriction.getDomainConceptId(), attributeRestriction);
		}
		return map;
	}

}