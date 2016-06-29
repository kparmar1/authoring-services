package org.ihtsdo.snowowl.authoring.single.api.batchImport.pojo;

import java.util.HashMap;
import java.util.Map;

import org.ihtsdo.otf.rest.exception.BusinessServiceException;
import org.ihtsdo.snowowl.authoring.single.api.batchImport.service.BatchImportService;

import com.b2international.snowowl.snomed.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.Acceptability;

public class BatchImportTerm {
	String term;
	boolean isFSN;
	char acceptabilityUS='N';
	char acceptabilityGB='N';
	String caseSensitivity;
	
	public BatchImportTerm(String term, boolean isFSN, char usAccept, char gbAccept) {
		this.term = term;
		this.isFSN = isFSN;
		this.acceptabilityUS = usAccept;
		this.acceptabilityGB = gbAccept;
	}
	
	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		this.term = term;
	}
	public boolean isFSN() {
		return isFSN;
	}
	public void setFSN(boolean isFSN) {
		this.isFSN = isFSN;
	}
	public char getAcceptabilityUS() {
		return acceptabilityUS;
	}
	public void setAcceptabilityUS(char acceptabilityUS) {
		this.acceptabilityUS = acceptabilityUS;
	}
	public char getAcceptabilityGB() {
		return acceptabilityGB;
	}
	public void setAcceptabilityGB(char acceptabilityGB) {
		this.acceptabilityGB = acceptabilityGB;
	}
	public String getCaseSensitivity() {
		return caseSensitivity;
	}
	public void setCaseSensitivity(String caseSensitivity) {
		this.caseSensitivity = caseSensitivity;
	}

}
