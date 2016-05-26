package org.ihtsdo.termserver.scripting.fixes;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.ihtsdo.termserver.scripting.client.SnowOwlClient.ExportType;
import org.ihtsdo.termserver.scripting.client.SnowOwlClient.ExtractType;
import org.ihtsdo.termserver.scripting.client.SnowOwlClientException;
import org.ihtsdo.termserver.scripting.domain.Batch;
import org.ihtsdo.termserver.scripting.domain.Concept;
import org.ihtsdo.termserver.scripting.domain.RF2Constants;
import org.ihtsdo.termserver.scripting.domain.Relationship;
import org.ihtsdo.termserver.scripting.fixes.BatchFix.REPORT_ACTION_TYPE;

import us.monoid.json.JSONObject;

import com.b2international.commons.StringUtils;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

/*
Drug Product fix loads the input file, but only really works with the Medicinal Entities.
The full project hierarchy is exported and loaded locally so we can scan for other concepts
that have the exact same combination of ingredients.
These same ingredient concepts are worked on together in a single task.
What rules are applied to each one depends on the type - Medicinal Entity, Product Strength, Medicinal Form
 */
public class DrugProductFix extends BatchFix implements RF2Constants{
	
	protected DrugProductFix(BatchFix clone) {
		super(clone);
	}

	private static final String SEPARATOR = "_";
	
	ProductStrengthFix psf;
	MedicinalFormFix mff;
	MedicinalEntityFix mef;
	GrouperFix gf;
	
	public static void main(String[] args) throws TermServerFixException, IOException, SnowOwlClientException {
		DrugProductFix fix = new DrugProductFix(null);
		fix.init(args);
		//Recover the current project state from TS (or local cached archive) to allow quick searching of all concepts
		fix.loadProject();
		fix.processFile();
	}
	
	protected void init(String[] args) throws TermServerFixException, IOException {
		super.init(args);
		psf = new ProductStrengthFix(this);
		mff = new MedicinalFormFix(this);
		mef = new MedicinalEntityFix(this);
		gf =  new GrouperFix(this);
	}

	@Override
	public int doFix(Batch batch, Concept concept) throws TermServerFixException {
		Concept loadedConcept = loadConcept(concept, batch.getBranchPath());
		loadedConcept.setConceptType(concept.getConceptType());
		int changesMade = 0;
		switch (concept.getConceptType()) {
			case MEDICINAL_ENTITY : changesMade = mef.doFix(batch, loadedConcept);
									break;
			case MEDICINAL_FORM : changesMade = mff.doFix(batch, loadedConcept);
									break;
			case PRODUCT_STRENGTH : changesMade = psf.doFix(batch, loadedConcept);
									break;
			case GROUPER : changesMade = gf.doFix(batch, loadedConcept);
									break;
			case PRODUCT_ROLE : 
			default : warn ("Don't know what to do with " + concept);
			report(batch, concept, REPORT_ACTION_TYPE.VALIDATION_ERROR, "Concept Type not determined.");
		}
		try {
			String conceptSerialised = gson.toJson(loadedConcept);
			debug ("Updating state of " + loadedConcept);
			tsClient.updateConcept(new JSONObject(conceptSerialised), batch.getBranchPath());
		} catch (Exception e) {
			report(batch, concept, REPORT_ACTION_TYPE.API_ERROR, "Failed to save changed concept to TS: " + e.getMessage());
		}
		return changesMade;
	}

	@Override
	List<Batch> formIntoBatches(String fileName, List<Concept> concepts, String branchPath) throws TermServerFixException {
		List<Batch> batches = new ArrayList<Batch>();
		debug ("Finding all concepts with ingredients...");
		Multimap<String, Concept> ingredientCombos = findAllIngredientCombos();
		
		//If the concept is of type Medicinal Entity, then put it in a batch with other concept with same ingredient combo
		for (Concept thisConcept : concepts) {
			if (thisConcept.getConceptType().equals(ConceptType.MEDICINAL_ENTITY)) {
				List<Relationship> ingredients = getIngredients(thisConcept);
				String comboKey = getIngredientCombinationKey(thisConcept, ingredients);
				//Get all concepts with this identical combination of ingredients
				Collection<Concept> matchingCombo = ingredientCombos.get(comboKey);
				Batch thisComboBatch = new Batch();
				thisComboBatch.setDescription(getIngredientList(ingredients));
				thisComboBatch.setConcepts(new ArrayList<Concept>(matchingCombo));
				batches.add(thisComboBatch);
				debug ("Batched " + thisConcept + " with " + comboKey.split(SEPARATOR).length + " active ingredients.  Batch size " + matchingCombo.size());
			} else {
				//Validate that concept does have a type and some ingredients otherwise it's going to get missed
				if (thisConcept.getConceptType().equals(ConceptType.UNKNOWN)) {
					warn ("Concept is of unknown type: " + thisConcept);
				}
				
				if (getIngredients(thisConcept).size() == 0) {
					warn ("Concept has no ingredients: " + thisConcept);
				}
			}
		}
		return batches;
	}
	
	private List<Relationship> getIngredients(Concept c) {
		return c.getRelationships(CHARACTERISTIC_TYPE.INFERRED_RELATIONSHIP, HAS_ACTIVE_INGRED, ACTIVE_STATE.ACTIVE);
	}
	
	private String getIngredientList(List<Relationship> ingredientRelationships) {
		ArrayList<String> ingredientNames = new ArrayList<String>();
		for (Relationship r : ingredientRelationships) {
			String ingredientName = r.getTarget().getFsn().replaceAll("\\(.*?\\)","").trim();
			ingredientNames.add(ingredientName);
		}
		Collections.sort(ingredientNames);
		return ingredientNames.toString().replaceAll("\\[|\\]", "").replaceAll(", "," + ");
	}
	
	private Multimap<String, Concept> findAllIngredientCombos() throws TermServerFixException {
		Collection<Concept> allConcepts = GraphLoader.getGraphLoader().getAllConcepts();
		Multimap<String, Concept> ingredientCombos = ArrayListMultimap.create();
		for (Concept thisConcept : allConcepts) {
			List<Relationship> ingredients = thisConcept.getRelationships(CHARACTERISTIC_TYPE.INFERRED_RELATIONSHIP, HAS_ACTIVE_INGRED, ACTIVE_STATE.ACTIVE);
			if (ingredients.size() > 0) {
				String comboKey = getIngredientCombinationKey(thisConcept, ingredients);
				ingredientCombos.put(comboKey, thisConcept);
			}
		}
		return ingredientCombos;
	}

	private void loadProject() throws SnowOwlClientException, TermServerFixException {
		File snapShotArchive = new File (project + ".zip");
		//Do we already have a copy of the project locally?  If not, recover it.
		if (!snapShotArchive.exists()) {
			println ("Recovering current state of " + project + " from TS");
			tsClient.export("MAIN/" + project, null, ExportType.MIXED, ExtractType.SNAPSHOT, snapShotArchive);
		}
		GraphLoader gl = GraphLoader.getGraphLoader();
		println ("Loading archive contents into memory...");
		try {
			ZipInputStream zis = new ZipInputStream(new FileInputStream(snapShotArchive));
			ZipEntry ze = zis.getNextEntry();
			try {
				while (ze != null) {
					if (!ze.isDirectory()) {
						Path p = Paths.get(ze.getName());
						String fileName = p.getFileName().toString();
						if (fileName.contains("sct2_Relationship_Snapshot")) {
							println("Loading Relationship File.");
							gl.loadRelationshipFile(zis);
						} else if (fileName.contains("sct2_Description_Snapshot")) {
							println("Loading Description File.");
							gl.loadDescriptionFile(zis);
						}
					}
					ze = zis.getNextEntry();
				}
			} finally {
				try{
					zis.closeEntry();
					zis.close();
				} catch (Exception e){} //Well, we tried.
			}
		} catch (IOException e) {
			throw new TermServerFixException("Failed to extract project state from archive " + snapShotArchive.getName(), e);
		}
	}

	private String getIngredientCombinationKey(Concept loadedConcept, List<Relationship> ingredients) throws TermServerFixException {
		String comboKey = "";
		for (Relationship r : ingredients) {
			if (r.isActive()) {
				comboKey += r.getTarget().getConceptId() + SEPARATOR;
			}
		}
		if (comboKey.isEmpty()) {
			println ("*** Unable to find ingredients for " + loadedConcept);
			comboKey = "NONE";
		}
		return comboKey;
	}

	@Override
	public String getFixName() {
		return "MedicinalEntity";
	}
}