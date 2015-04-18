package org.ihtsdo.snowowl.authoring.api.services;

import com.google.common.io.Files;
import org.ihtsdo.snowowl.authoring.api.model.logical.IsARestriction;
import org.ihtsdo.snowowl.authoring.api.model.logical.LogicalModel;
import org.ihtsdo.snowowl.authoring.api.model.logical.RangeRelationType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations={"/test-context.xml"})
public class AuthoringServiceTest {

	@Autowired
	private AuthoringService authoringService;

	@Before
	public void setUp() throws Exception {
		authoringService.setBaseFilesDirectory(Files.createTempDir());
	}

	@Test
	public void testSaveLoadLogicalModel() throws IOException {
		String modelName = "isAOnlyModel";
		LogicalModel logicalModel = new LogicalModel(modelName, new IsARestriction("123", RangeRelationType.SELF));
		authoringService.saveLogicalModel(logicalModel);

		LogicalModel logicalModel1 = authoringService.loadLogicalModel(modelName);

		Assert.assertNotSame(logicalModel, logicalModel1);
		Assert.assertTrue(logicalModel.equals(logicalModel1));
	}

}