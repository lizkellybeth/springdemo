package edu.nrao.companalyst;

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


@RestController
@CrossOrigin(origins = "http://localhost:4200")// allows local debugging with Angular
@EnableAutoConfiguration
public class CompAnalystController {
	
	private CompAnalystCache cache;
	
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
    
	@Value("${companalyst.jedis.host}")
    private String jedisHost;
    
	@Value("${companalyst.jedis.port}")
    private int jedisPort;
    
	@Value("${companalyst.jedis.timeout}")
    private int jedisTimeout;
    
    private String authToken;
    private Date expireDate;

	public CompAnalystController() {
		super();
	}
	
	private CompAnalystCache getCache() {
		if (this.cache == null) {
			this.cache = new CompAnalystCache(jedisHost, jedisPort, jedisTimeout);
		}
		return cache;
	}

	private void checkApiAuthentication() {
        Date currDate = new Date();
        if ((authToken == null) || (expireDate == null) || (currDate.after(expireDate))) {
        	expireDate = null;
        	authToken = null;
        	getApiToken();
        }
	}

    @RequestMapping(value = "/hello", produces = { "application/json" })
    public String someMethodName() {
		return "{hello}";
	}

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
    public String getCompanyJob(@RequestParam(required = true) String jobcode) throws Exception {
    	System.out.println("jobcode parameter: [" + jobcode + "]");
    	String json = getCache().getCompanyJob(jobcode);
    	if (json == null) {
    		json =  getCompanyJob(COMPURL_COMPANYJOB, jobcode);
    		getCache().saveCompanyJob(jobcode, json);
    	}
        return json;
    }
    
    @RequestMapping(value = "/companyjoblist", produces = { "application/json" })
    public String getCompanyJobList() throws Exception {
    	String json = getCache().getCompanyJobList();
    	if (json == null) {
    		json =  getJson(COMPURL_COMPANYJOBLIST);
    		getCache().saveCompanyJobList(json);
    	}
        return json;
    }
    
    @RequestMapping(value = "/employeelist", produces = { "application/json" })
    public String getEmployeeList() throws Exception {
    	String json = getCache().getCompanyJobList();
    	if (json == null) {
    		json =  getJson(COMPURL_EMPLOYEELIST);
    		//need to secure redis server before caching this
    		//getCache().saveEmployeeList(json);
    	}
        return json;
    }
    
    //example http://localhost:8080/compensationjoblist
    @RequestMapping(value = "/compensationjoblist", produces = { "application/json" })
    public String getCompensationJobList() throws Exception {
    	String json = getCache().getCompensationJobList();
    	if (json == null) {
    		json =  getJson(COMPURL_COMPENSATIONJOBLIST);
    		HashMap<String, String> params = buildCompensationJobListParameterMap();
    		json = post(COMPURL_COMPENSATIONJOBLIST, params);
    		getCache().saveCompensationJobList(json);
    	}
        return json;
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
    
    private String post(String url, HashMap<String, String> params) throws Exception {
    	HttpClient client = HttpClientBuilder.create().build();
        HttpPost post = new HttpPost(url);
        post.setHeader("Content-Type", "application/json");
        checkApiAuthentication();
        post.addHeader("token", authToken);
        JSONObject obj = new JSONObject(params);
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
    

}
