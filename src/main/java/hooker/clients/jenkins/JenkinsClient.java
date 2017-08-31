package hooker.clients.jenkins;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

@EqualsAndHashCode
@ToString(exclude = {"apiToken", "githubToken"})
public class JenkinsClient {

    private static final Logger LOGGER = Logger.getLogger( JenkinsClient.class.getName() );

    private static final String JOB = "org.jenkinsci.plugins.workflow.job.WorkflowJob";
    private static final String MULTI_BRANCH_PROJECT = "org.jenkinsci.plugins.workflow.multibranch.WorkflowMultiBranchProject";

    private static final String GET = "get";
    private static final String POST = "post";

    private final Gson gson = new Gson();

    @Getter @Setter
    private String url;

    @Getter @Setter
    private String user;

    @Setter
    private String apiToken;

    @Setter
    private String githubToken;


    public JenkinsClient() {}

    public JenkinsClient(String url, String user, String apiToken, String githubToken){
        this.url = url;
        this.user = user;
        this.apiToken = apiToken;
        this.githubToken = githubToken;
    }

    public boolean triggerJob(String service, String branch, String deliveryId){
        HttpResponse response;
        String build_url = this.url + "/job/" + service;
        JenkinsInfo info = this.describeJobs();
        JenkinsJob job = null;

        if(info != null){
            job = info.getJobs().stream()
                    .filter(x -> service.equals(x.getName()))
                    .findFirst()
                    .orElse(null);
        }

        if(job == null) {
            LOGGER.log(Level.WARNING, "Could not find job " + service + " on the Jenkins Server. JenkinsClient: " + this.toString());
            return false;
        }

        String queryParams = "?token=" + this.githubToken + "&hookid=" + deliveryId + "&branch=" + branch;

        if(job.getJenkinsClass().equals(JOB)){
            build_url += "/buildWithParameters" + queryParams;
        } else if(job.getJenkinsClass().equals(MULTI_BRANCH_PROJECT)){
            try {
                build_url += "/job/" + URLEncoder.encode(branch, "UTF-8") + "/build" + queryParams;
            } catch(UnsupportedEncodingException uee){
                LOGGER.log(Level.SEVERE, "Unsupported Encoding Exception. " + uee.getMessage());
                return false;
            }
        }

        response = this.callJenkins(POST, build_url);
        LOGGER.log(Level.INFO, "Received Response ("+ response.getStatus() + ": " + response.getStatusText() + ") after trigger job: " + service + ". Response: " + response.getBody());

        return true;
    }

    public JenkinsInfo describeJobs(){
        final String info_url =  this.url + "/api/json";
        HttpResponse response = null;
        JenkinsInfo jenkinsInfo = null;

        response = this.callJenkins(GET, info_url);

        if(response.getStatus() >= 200 && response.getStatus() < 300){
            LOGGER.log(Level.FINE, "Described Jenkins Jobs: " + response.getBody().toString());
        }else{
            LOGGER.log(Level.SEVERE, "Received Status: " + response.getStatus() + ": " + response.getStatusText() + " from Jenkins when trying to describe jobs at " + info_url);
            return null;
        }

        try{
            jenkinsInfo = gson.fromJson(response.getBody().toString(), JenkinsInfo.class);
        }catch (JsonParseException jpe){
            LOGGER.log(Level.SEVERE, "Couldn't create JenkinsInfo object from json response. GSON Exception: " + jpe.getMessage());
        }catch (Exception e){
            LOGGER.log(Level.SEVERE, "Unhandled exception when describing Jenkins Jobs");
        }
        return jenkinsInfo;
    }

    private HttpResponse callJenkins(String httpMethod, String requestUrl){
        final String[] csrf_header_kv = this.retrieveCSRFCrumb();
        HttpResponse response = null;

        try {
            if (httpMethod.toLowerCase().equals(GET)) {
                response = Unirest.get(requestUrl).basicAuth(this.user, this.apiToken).header(csrf_header_kv[0], csrf_header_kv[1]).asString();
            } else if (httpMethod.toLowerCase().equals(POST)) {
                response = Unirest.post(requestUrl).basicAuth(this.user, this.apiToken).header(csrf_header_kv[0], csrf_header_kv[1]).asString();
            } else {
                LOGGER.log(Level.SEVERE, "Unsupported HTTP Method passed into callJenkins.");
                return null;
            }
        }catch(UnirestException ue){
            LOGGER.log(Level.SEVERE, "There was a problem sending a " + httpMethod.toUpperCase() + " to the Jenkins URL " + requestUrl + "  endpoint. Client Instance: " + this.toString() + "\nUnirest Exception: " + ue.getMessage());
        }

        return response;
    }

    private String[] retrieveCSRFCrumb(){
        try {
            String csrf_crumb_url = this.url + "/crumbIssuer/api/xml?xpath=" + URLEncoder.encode("concat(//crumbRequestField,\":\",//crumb)", "UTF-8");
            HttpResponse csrfCrumbResponse = Unirest.get(csrf_crumb_url).basicAuth(this.user, this.apiToken).asString();
            return csrfCrumbResponse.getBody().toString().split(":");
        }catch(UnirestException ue) {
            LOGGER.log(Level.SEVERE, "There was a problem sending a GET request to the Jenkins CSRF Token endpoint. JenkinsClient Instance: " + this.toString() + "\nUnirest Exception: " + ue.getMessage());
        }catch(UnsupportedEncodingException uee){
            LOGGER.log(Level.SEVERE, "Unsupported Encoding Exception. " + uee.getMessage());
        } catch(Exception e){
            LOGGER.log(Level.SEVERE, "There was a problem parsing the response from the Jenkins CSRF Token endpoint. Exception: " + e.getMessage());
        }
        return null;
    }



}
