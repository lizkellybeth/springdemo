A. Setting up the basic development environment for a springboot webapp

1. Download and install STS 4.8.0 (spring tool suite). 
https://spring.io/tools

2. New -> Spring Boot -> Spring Starter Project
On the new project wizard, ignore everytrhing except:
Name: companalyst
Group: edu.nrao
Artifact: companalyst
Package: edu.nrao.companalyst

Click finish

3. Open the pom.xml. Add the dependency:
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-web</artifactId>
</dependency>


4. right-click project. 
Maven -> update project
wait a few seconds

5. Create a new Java class: CompAnalystController

6. add annotations:
@RestController
@CrossOrigin(origins = "http://localhost:4200") // allows local debugging with Angular
@EnableAutoConfiguration
public class CompAnalystController {

7. right click source -> organize imports

8. add a public String method, with annotation:
    @RequestMapping(value = "/hello", produces = { "application/json" })
    public String someMethodName() {
		return "{hello}";
	}

9. fix imports

10. right-click project, "Run as" -> "spring boot app"

11. open a web browser and go to 
http://localhost:8080/hello

12. now you have a functional springboot webapp. 

13. To deploy this for real, you open a terminal on the project directory where the pom.xml is located, and do:
$> mvn clean package

14. if you look now at the ../target/ directory, you'll see the runnable jar file
$> java -jar companalyst-0.0.1-SNAPSHOT.jar

15. you can copy that jar to a server and run it like this: (make sure eclipse isn't running it already)
java -jar companalyst-0.0.1-SNAPSHOT.jar


B. preparing for companalyst:

16. add the following depenencies to pom.xml:
		<dependency>
			<groupId>org.apache.httpcomponents</groupId>
			<artifactId>httpclient</artifactId>
		</dependency>

		<dependency>
			<groupId>org.springframework.boot</groupId>
			<artifactId>spring-boot-devtools</artifactId>
		</dependency>
		
		<dependency>
		    <groupId>org.json</groupId>
		    <artifactId>json</artifactId>
		    <version>20200518</version>
		</dependency>
		
17. right-click the project
Maven -> update project
wait a few seconds.

18. edit the application.properties file in src/main/resources/ as follows:
companalyst.url.getapitoken = https://companalystapi.salary.com/cloud/v1/GetApiToken
companalyst.url.companyjoblist =   https://companalystapi.salary.com/cloud/Data/V1/CompanyJobList
companalyst.url.employeelist =   https://companalystapi.salary.com/cloud/Data/V1/EmployeeList
companalyst.url.compensationjoblist = https://companalystapi.salary.com/cloud/Data/V1/CompensationJobList
companalyst.url.companyjob = https://companalystapi.salary.com/cloud/Data/V1/CompanyJob
companalyst.auth.username = xxxx
companalyst.auth.password = xxxx

18.1 FIX username and password!

19. add the following additional code to CompAnalystController:

	@Value("${companalyst.url.getapitoken}")
	private String COMPURL_GETAPITOKEN;
	
	@Value("${companalyst.url.companyjob}")
	private String COMPURL_COMPANYJOB;
    
	@Value("${companalyst.url.companyjoblist}")
	private String COMPURL_COMPANYJOBLIST;
    
	@Value("${companalyst.url.employeelist}")
	private String COMPURL_EMPLOYEELIST;
    
	@Value("${companalyst.url.compensationjoblist}")
	private String COMPURL_COMPENSATIONJOBLIST;
    
	@Value("${companalyst.auth.username}")
    private String username;
    
	@Value("${companalyst.auth.password}")
    private String password;
    
    private String authToken;
    private Date expireDate;

    @RequestMapping(value = "/getapitoken", produces = { "application/json" })
    public String getApiToken() {
        try {
            HttpClient client = HttpClientBuilder.create().build();
            HttpPost post = new HttpPost(COMPURL_GETAPITOKEN);
            post.setHeader("Content-Type", "application/json");

            HashMap<String, String> map = new HashMap<String, String>();
			map.put("username", username);	
			map.put("password", password);	
	        JSONObject obj = new JSONObject(map);
	        String json = obj.toString();
	        HttpEntity entity = new ByteArrayEntity(json.getBytes("UTF-8"));
	        post.setEntity(entity);
            
            HttpResponse response = client.execute(post);
            String result = EntityUtils.toString(response.getEntity());
            System.out.println("result: " + result); 
            JSONObject jobj = new JSONObject(result);
            authToken = (String) jobj.get("token");
            String expires = (String) jobj.get("expire_date");
            SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy hh:mm:ss a");
            expireDate = sdf.parse(expires);
            String output = jobj.toString();
            return output;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    //example http://localhost:8080/companyjob?jobcode=FN5502
    @RequestMapping(value = "/companyjob", produces = { "application/json" })
    public String getCompanyJobList(@RequestParam(required = true) String jobcode) throws Exception {
    	System.out.println("jobcode parameter: [" + jobcode + "]");
        String result =  getCompanyJob(COMPURL_COMPANYJOB, jobcode);
        return result;
    }
    
    @RequestMapping(value = "/companyjoblist", produces = { "application/json" })
    public String getCompanyJobList() throws Exception {
    	String result =  getJson(COMPURL_COMPANYJOBLIST);
        return result;
    }
    
    @RequestMapping(value = "/employeelist", produces = { "application/json" })
    public String getEmployeeList() throws Exception {
        String result = getJson(COMPURL_EMPLOYEELIST);
        return result;
    }
    
    @RequestMapping(value = "/compensationjoblist", produces = { "application/json" })
    public String getCompensationJobList() throws Exception {
    	
        String result = post(COMPURL_COMPENSATIONJOBLIST);
        return result;
    }
    
    private String getJson(String url) throws Exception {
    	HttpClient client = HttpClientBuilder.create().build();
        HttpGet get = new HttpGet(url);
        get.setHeader("Content-Type", "application/json");
        checkApiAuthentication();
        get.addHeader("token", authToken);
        HttpResponse response = client.execute(get);
        String result = EntityUtils.toString(response.getEntity());
        return result;
    }
    
    //example http://localhost:8080/companyjob?jobcode=FN5502
    private String getCompanyJob(String url, String jobCode) throws Exception {
    	HttpClient client = HttpClientBuilder.create().build();
    	jobCode = URLEncoder.encode(jobCode, StandardCharsets.UTF_8);
    	url = url + "/" + jobCode;
    	HttpGet get = new HttpGet(url);        
        get.setHeader("Content-Type", "application/json");
        checkApiAuthentication();
        get.addHeader("token", authToken);
        HttpResponse response = client.execute(get);
        String result = EntityUtils.toString(response.getEntity());
        return result;
    }
    
    private String post(String url) throws Exception {
    	HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/json");
        checkApiAuthentication();
        post.addHeader("token", authToken);
        HashMap<String, String> map = buildCompensationJobListParameterMap();
        JSONObject obj = new JSONObject(map);
        String json = obj.toString();
        HttpEntity entity = new ByteArrayEntity(json.getBytes("UTF-8"));
        post.setEntity(entity);
        HttpResponse response = client.execute(post);
        String result = EntityUtils.toString(response.getEntity());
        System.out.println("result: " + result); 	
        return result;
    }
    
	private HashMap<String, String> buildCompensationJobListParameterMap() {
			HashMap<String, String> map = new HashMap<String, String>();
			map.put("JobKeyword", "Job Keyword");	
			/**
			map.put("LCID", null);	
			map.put("JobDesc", null);	
			map.put("CountryCode", null);	
			map.put("EducationKeyword", null);	
			map.put("LicenseKeyword", null);	
			map.put("CertificateKeyword", null);	
			map.put("ReportToKeyword", null);	
			map.put("YOEMin", null);	
			map.put("YOEMax", null);	
			map.put("Location", null);	
			map.put("FTESize", null);	
			map.put("Industry", null);	
		*/
			return map;
	}
    
	private void checkApiAuthentication() {
        Date currDate = new Date();
        if ((authToken == null) || (expireDate == null) || (currDate.after(expireDate))) {
        	expireDate = null;
        	authToken = null;
        	getApiToken();
        }
	}


20. right click source -> organize imports 
Choose these:

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


21. all compile errors shpuld be eliminated.

22. right-click project, Run as -> spring boot app

23. open a browser (preferably Firefox to show the JSON better:
http://localhost:8080/getapitoken 

24. It works!!


