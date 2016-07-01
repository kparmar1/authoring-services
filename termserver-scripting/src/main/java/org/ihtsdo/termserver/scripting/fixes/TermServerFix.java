package org.ihtsdo.termserver.scripting.fixes;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.ihtsdo.termserver.scripting.client.SCAClient;
import org.ihtsdo.termserver.scripting.client.SnowOwlClient;
import org.ihtsdo.termserver.scripting.domain.RF2Constants;

import us.monoid.web.Resty;

public abstract class TermServerFix implements RF2Constants {
	
	static boolean debug = true;
	static boolean dryRun = true;
	static int dryRunCounter = 0;
	static int throttle = 0;
	protected String url = environments[0];
	protected SnowOwlClient tsClient;
	protected SCAClient scaClient;
	protected String authenticatedCookie;
	protected Resty resty = new Resty();
	protected String project;
	public static final int maxFailures = 5;
	protected int restartPosition = NOT_SET;
	private static Date startTime;
	private static Map<String, Object> summaryDetails = new HashMap<String, Object>();
	private static String summaryText = "";
	
	public static String CONCEPTS_IN_FILE = "Concepts in file";
	public static String CONCEPTS_PROCESSED = "Concepts processed";
	public static String REPORTED_NOT_PROCESSED = "Reported not processed";
	public static String CRITICAL_ISSUE = "CRITICAL ISSUE";

	public abstract String getFixName();
	
	public String getAuthenticatedCookie() {
		return authenticatedCookie;
	}
	
	public static int getNextDryRunNum() {
		return ++dryRunCounter;
	}

	public void setAuthenticatedCookie(String authenticatedCookie) {
		this.authenticatedCookie = authenticatedCookie;
	}

	private static String[] environments = new String[] {	"http://localhost:8080/",
															"https://dev-term.ihtsdotools.org/",
															"https://uat-term.ihtsdotools.org/",
															"https://term.ihtsdotools.org/",
	};
	
	public static void println (String msg) {
		System.out.println (msg);
	}
	
	public static void print (String msg) {
		System.out.print (msg);
	}
	
	public static void debug (String msg) {
		System.out.println (msg);
	}
	
	public static void warn (String msg) {
		System.out.println ("*** " + msg);
	}
	
	public static String getMessage (Exception e) {
		String msg = e.getMessage();
		Throwable cause = e.getCause();
		if (cause != null) {
			msg += " caused by " + cause.getMessage();
		}
		return msg;
	}
	
	public void init() {
		println ("Select an environment ");
		for (int i=0; i < environments.length; i++) {
			println ("  " + i + ": " + environments[i]);
		}
		try (Scanner in = new Scanner(System.in)) {
			print ("Choice: ");
			String choice = in.nextLine().trim();
			url = environments[Integer.parseInt(choice)];
		
			tsClient = new SnowOwlClient(url + "snowowl/snomed-ct/v2", "snowowl", "snowowl");
			if (authenticatedCookie == null) {
				print ("Please enter your authenticated cookie for connection to " + url + " : ");
				authenticatedCookie = in.nextLine().trim();
			}
			//TODO Make calls through client objects rather than resty direct and remove this member 
			resty.withHeader("Cookie", authenticatedCookie);  
			scaClient = new SCAClient(url, authenticatedCookie);
			
			print ("Specify Project " + (project==null?": ":"[" + project + "]: "));
			String response = in.nextLine().trim();
			if (!response.isEmpty()) {
				project = response;
			}
			
			if (restartPosition != NOT_SET) {
				print ("Restarting from line [" +restartPosition + "]: ");
				response = in.nextLine().trim();
				if (!response.isEmpty()) {
					restartPosition = Integer.parseInt(response);
				}
			}
			
			if (throttle > 0) {
				print ("Time delay between tasks (throttle) seconds [" +throttle + "]: ");
				response = in.nextLine().trim();
				if (!response.isEmpty()) {
					throttle = Integer.parseInt(response);
				}
			}

		}
		if (restartPosition == 0) {
			println ("Restart position given as 0 but line numbering starts from 1.  Starting at line 1.");
			restartPosition = 1;
		}
	}
	
	public String getProject() {
		return project;
	}

	public void setProject(String project) {
		this.project = project;
	}
	
	public void startTimer() {
		startTime = new Date();
	}
	
	public void addSummaryInformation(String item, Object detail) {
		summaryDetails.put(item, detail);
	}
	
	public void incrementSummaryInformation(String key, int incrementAmount) {
		if (!summaryDetails.containsKey(key)) {
			summaryDetails.put (key, new Integer(0));
		}
		int newValue = ((Integer)summaryDetails.get(key)).intValue() + incrementAmount;
		summaryDetails.put(key, newValue);
	}
	
	public void storeRemainder(String start, String remove1, String remove2, String storeAs) {
		Collection<?> differences = new ArrayList((Collection<?>)summaryDetails.get(start));
		Collection<?> removeList = (Collection<?>)summaryDetails.get(remove1);
		differences.removeAll(removeList);
		if (remove2 != null && !remove2.isEmpty()) {
			removeList = (Collection<?>)summaryDetails.get(remove2);
			differences.removeAll(removeList);
		}
		summaryDetails.put(storeAs, differences.toString());
	}
	
	public void finish() {
		println ("===========================================");
		Date endTime = new Date();
		SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
		//I've not had to adjust for timezones when creating a date before?
		if (startTime != null) {
			Date diff = new Date(endTime.getTime() - startTime.getTime() + (endTime.getTimezoneOffset() * 60 * 1000));
			recordSummaryText ("Completed processing in " + sdf.format(diff));
			recordSummaryText ("Started at: " + startTime);
		}
		recordSummaryText ("Finished at: " + endTime);
		List<String> criticalIssues = new ArrayList<String>();
		
		for (Map.Entry<String, Object> summaryDetail : summaryDetails.entrySet()) {
			String key = summaryDetail.getKey();
			Object value = summaryDetail.getValue();
			String display = "";
			if (value instanceof Collection) {
				display += ((Collection<?>)value).size();
			} else if (key.startsWith(CRITICAL_ISSUE)) {
				criticalIssues.add(key + ": " + value.toString());
				continue;
			} else {
				display = value.toString();
			}
			recordSummaryText (key + ": " + display);
		}
		if (summaryDetails.containsKey("Tasks created") && summaryDetails.containsKey(CONCEPTS_PROCESSED) ) {
			double c = (double)((Collection)summaryDetails.get("Concepts processed")).size();
			double t = (double)((Integer)summaryDetails.get("Tasks created")).intValue();
			double avg = Math.round((c/t) * 10) / 10.0;
			recordSummaryText ("Concepts per task: " + avg);
		}
		
		if (criticalIssues.size() > 0) {
			recordSummaryText ("Critical Issues Encountered\n========================");
			for (String thisCriticalIssue : criticalIssues) {
				recordSummaryText(thisCriticalIssue);
			}
		}
		
	}
	
	private synchronized void recordSummaryText(String msg) {
		println (msg);
		msg = msg.replace("\n", "\n</br>");
		summaryText += msg + "\n<br/>";
	}
	
	public static String getSummaryText() {
		return summaryText;
	}

}
