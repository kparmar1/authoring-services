package org.ihtsdo.snowowl.authoring.api.services;

import com.b2international.snowowl.api.domain.IComponentRef;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.ihtsdo.snowowl.authoring.api.model.AuthoringContent;
import org.ihtsdo.snowowl.authoring.api.model.AuthoringContentValidationResult;
import org.ihtsdo.snowowl.authoring.api.model.logical.LogicalModel;
import org.ihtsdo.snowowl.authoring.api.model.logical.LogicalModelValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AuthoringService {

	public static final String JSON_EXTENTION = ".json";
	@Autowired
	private LogicalModelValidator logicalModelValidator;

	@Autowired
	private ObjectMapper jsonMapper;

	@Autowired
	private ContentService contentService;

	private File baseFilesDirectory;

	private Logger logger = LoggerFactory.getLogger(getClass());

	public AuthoringService() {
		this(new File("")); // Create with default base directory
	}

	public AuthoringService(File baseFilesDirectory) {
		this.baseFilesDirectory = baseFilesDirectory;
	}

	public void saveLogicalModel(LogicalModel logicalModel) throws IOException {
		String name = logicalModel.getName();
		Assert.notNull(name, "Logical model name can not be null.");
		try (FileWriter writer = new FileWriter(getLogicalModelFile(name))) {
			jsonMapper.writeValue(writer, logicalModel);
		}
	}

	public LogicalModel loadLogicalModel(String name) throws IOException {
		Assert.notNull(name, "Logical model name can not be null.");
		File logicalModelFile = getLogicalModelFile(name);
		if (logicalModelFile.isFile()) {
			try (FileReader src = new FileReader(logicalModelFile)) {
				return jsonMapper.readValue(src, LogicalModel.class);
			}
		} else {
			throw new LogicalModelNotFoundException(name);
		}
	}

	public List<String> listLogicalModelNames() {
		File logicalModelsDirectory = getLogicalModelsDirectory();
		File[] files = logicalModelsDirectory.listFiles();

		logger.info("logicalModelsDirectory {}", logicalModelsDirectory);
		logger.info("files {}", (Object[])files);

		List<String> names = new ArrayList<>();
		for (File file : files) {
			String name = file.getName();
			logger.info("Found file {}", name);
			if (name.endsWith(JSON_EXTENTION)) {
				names.add(name.replaceFirst("\\.json$", ""));
			}
		}
		logger.info("Names {}", names);
		return names;
	}

	public List<AuthoringContentValidationResult> validateContent(String logicalModelName, List<AuthoringContent> content) throws IOException {
		List<AuthoringContentValidationResult> results = new ArrayList<>();
		for (AuthoringContent authoringContent : content) {
			results.add(validateContent(logicalModelName, authoringContent));
		}
		return results;
	}

	public AuthoringContentValidationResult validateContent(String logicalModelName, AuthoringContent content) throws IOException {
		LogicalModel logicalModel = loadLogicalModel(logicalModelName);
		return logicalModelValidator.validate(content, logicalModel);
	}

	public Set<String> getDescendantIds(final IComponentRef ref) {
		return contentService.getDescendantIds(ref);
	}

	private File getLogicalModelFile(String name) {
		File logicalModelsDirectory = getLogicalModelsDirectory();
		return new File(logicalModelsDirectory, name + JSON_EXTENTION);
	}

	private File getLogicalModelsDirectory() {
		File logicalModelsDirectory = new File(baseFilesDirectory, "resources/org.ihtsdo.snowowl.authoring/logical-models");
		if (!logicalModelsDirectory.isDirectory()) {
			logicalModelsDirectory.mkdirs();
		}
		return logicalModelsDirectory;
	}

	public void setBaseFilesDirectory(File baseFilesDirectory) {
		this.baseFilesDirectory = baseFilesDirectory;
	}

}