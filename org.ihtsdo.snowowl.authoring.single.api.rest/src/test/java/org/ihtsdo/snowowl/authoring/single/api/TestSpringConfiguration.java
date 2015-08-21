package org.ihtsdo.snowowl.authoring.single.api;

import org.ihtsdo.snowowl.authoring.single.api.review.pojo.AuthoringTaskReview;
import org.ihtsdo.snowowl.authoring.single.api.review.service.ReviewService;
import org.ihtsdo.snowowl.authoring.single.api.service.BranchService;
import org.ihtsdo.snowowl.authoring.single.api.service.ServiceException;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@ComponentScan(basePackages = "org.ihtsdo.snowowl.authoring.single.api.review")
@SpringBootApplication
@Configuration
public class TestSpringConfiguration {

	/**
	 * Context Test Method
	 * TODO - not yet working
	 * @param args
	 */
	public static void main(String[] args) {
		final ApplicationContext applicationContext = new ClassPathXmlApplicationContext(new String [] {"review-context.xml"},
				new AnnotationConfigApplicationContext(TestSpringConfiguration.class));

		final ReviewService reviewService = applicationContext.getBean(ReviewService.class);
	}

	@Bean
	public BranchService getBranchService() {
		return new BranchService() {
			@Override
			public void createTaskBranchAndProjectBranchIfNeeded(String projectKey, String taskKey) throws ServiceException {

			}

			@Override
			public AuthoringTaskReview diffTaskBranch(String projectKey, String taskKey, List<Locale> locales) throws ExecutionException, InterruptedException {
				return null;
			}

			@Override
			public String getTaskPath(String projectKey, String taskKey) {
				return null;
			}

			@Override
			public String getProjectPath(String projectKey) {
				return null;
			}
		};
	}

}