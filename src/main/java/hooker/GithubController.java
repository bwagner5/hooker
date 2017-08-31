package hooker;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.google.gson.Gson;
import hooker.clients.github.Commit;
import hooker.clients.github.GitHubClient;
import hooker.clients.github.GithubHook;
import hooker.clients.github.PullRequestHook;
import hooker.clients.jenkins.JenkinsClient;
import hooker.clients.jenkins.JenkinsJob;
import org.apache.commons.codec.digest.HmacUtils;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Controller
@RequestMapping("/github")
public class GithubController {

    private static final Logger log = LoggerFactory.getLogger(GithubController.class);

    private final Gson gson = new Gson();

    private final String awsS3HookInfoBucket;
    private final String repoOwner;
    private final String repoName;

    private final Config config;
    private final JenkinsClient jenkinsClient;

    @Autowired
    public GithubController(Config config){
        this.config = config;
        this.jenkinsClient = new JenkinsClient(config.getJenkinsUrl(), config.getJenkinsUser(), config.getJenkinAPIToken(), config.getGithubToken());
        this.awsS3HookInfoBucket = config.getS3HookInfoBucket();
        this.repoOwner = config.getRepoOwner();
        this.repoName = config.getRepoName();
    }

    @RequestMapping(method= RequestMethod.POST)
    public @ResponseBody
    String hook(HttpServletRequest request, @RequestBody String body){

        String deliveryId = request.getHeader("X-Github-Delivery");
        String githubSignature = request.getHeader("X-Hub-Signature");


        if(deliveryId == null || githubSignature == null){
            log.error("A request was sent without X-Github-Delivery or X-Hub-Signature headers. This could be malicious traffic.");
            return "";
        }

        //Wrap in try-catch to prevent exception information from being sent back by spring boot to a malicious actor
        try {

            // Check the X-Hub-Signature
            if (!this.verifyGithubSignature(githubSignature, body)) {
                log.error("Github signature failed verification for deliveryId: " + deliveryId);
                return "";
            }

            log.info(String.format("Processing GitHub Hook with Delivery ID: %s", deliveryId));

            // navigate the webhook body with a Map
            Map<String, Object> mapType = new HashMap<>();
            Map<String, String> requestBodyMap = gson.fromJson(body, mapType.getClass());

            // Perform Pull Request Logic to run tests and send back status
            if (requestBodyMap.containsKey("pull_request")) {
                this.processPullRequest(body, deliveryId, requestBodyMap);
            }
            // Run a regular push webhook (usually used for a deployment)
            else {
                this.processPush(body, deliveryId);
            }

        } catch (Exception e){
            log.error("There was a problem processing the github hook for deliveryId: " + deliveryId);
        } finally {
            return "";
        }

    }

    private void processPush(String body, String deliveryId){
        GithubHook githubHook = gson.fromJson(body, GithubHook.class);
        if(githubHook.isDeleted() == true){
            log.info("Branch has been deleted, not triggering Jenkins");
            return;
        }

        sendCommitInfoToS3(githubHook.generateServiceInfoMap(), deliveryId);


        this.generateServicesToBuildSet(githubHook.aggregateFileModifications()).forEach(service->{
            try {
                if (this.jenkinsClient.triggerJob(service, githubHook.getBranch(), deliveryId) == false) {
                    log.error("JenkinsClient Returned False: There was a problem trigger: Service->{0} Branch->{1} DeliveryID->{2}", new Object[]{service, githubHook.getBranch(), deliveryId});
                }
            }catch(Exception e){
                log.error("EXCEPTION: There was a problem trigger: Service->{0} Branch->{1} DeliveryID->{2} \n EXCEPTION: {3}", new Object[]{service, githubHook.getBranch(), deliveryId, e.getMessage()});
            }
        });
    }

    private void processPullRequest(String body, String deliveryId, Map<String, String> requestBodyMap){
        String action = requestBodyMap.getOrDefault("action", "none");

        if(action.equalsIgnoreCase("opened") || action.equalsIgnoreCase("synchronize")){

            PullRequestHook pullRequestHook = gson.fromJson(body, PullRequestHook.class);

            GitHubClient gitHubClient = new GitHubClient(this.config.getGithubUsername(), this.config.getGithubPersonalAccessToken(), this.repoOwner, this.repoName);

            Set<String> fileChanges = gitHubClient.retrievePullRequestFileChanges(pullRequestHook.getNumber());

            Set<String> serviceCandidateSet = this.generateServicesToBuildSet(fileChanges);

            //Create hook info
            Map<String, ServiceInfo> changeMap = new ConcurrentHashMap<>();

            for(String service : serviceCandidateSet){
                Set<String> serviceChange = ConcurrentHashMap.newKeySet();
                serviceChange.add(service);
                changeMap.put(service, new ServiceInfo(service).addToCommits(new Commit(null, String.format("PR %s", String.valueOf(pullRequestHook.getNumber())), null,  true, serviceChange, null, null)));
            }

            this.sendCommitInfoToS3(changeMap, deliveryId);

            log.info(String.format("Triggering Service Candidate for PR %s: %s", pullRequestHook.getNumber(), serviceCandidateSet.toString()));

            for(String service : serviceCandidateSet){
                log.info("Attempting to trigger build candidate: " + service);
                this.jenkinsClient.triggerJob(service, String.valueOf(pullRequestHook.getNumber()), deliveryId);
            }

        }
    }

    private boolean verifyGithubSignature(String githubSignature, String body){
        String mySignature = HmacUtils.hmacSha1Hex(this.config.getGithubToken(), body);
        //gets hash function used, X-Hub-Signature is in the format "sha1=<sig>"
        String hashFunction = githubSignature.split("=")[0];
        mySignature = hashFunction + "=" + mySignature;

        if(mySignature.equals(githubSignature)){
            log.info("Signatures match, proceeding with hook");
            return true;
        } else{
            log.error("Signatures do not match! Terminating Hook!");
            return false;
        }
    }


    private String sendCommitInfoToS3(Map<String, ServiceInfo> serviceInfoMap, String deliveryId) {
        try{

            AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
            serviceInfoMap.forEach((service, info)->{
                info.filterCommitChangesets();
                Path path = Paths.get("/tmp/"+service+"_"+deliveryId+".json");
                String jsonInfo = gson.toJson(info);
                try {
                    File tempFile = File.createTempFile(service+"_"+deliveryId+".json", ".tmp");
                    tempFile.deleteOnExit();
                    FileUtils.writeStringToFile(tempFile, jsonInfo, "UTF-8");
                    s3Client.putObject(this.awsS3HookInfoBucket, deliveryId+"/"+service+".json", tempFile);
                    tempFile.delete();
                }catch(IOException ioe){
                    log.error("IO Exception when creating temp file with hook info to upload to S3");
                }
            });
        } catch(Exception e){
            log.error("There was a problem when trying to upload the hook info to S3");
            e.printStackTrace();
        }
        log.info(String.format("Posted GitHub hook data to S3 for hook delivery id: %s", deliveryId));
        return deliveryId;
    }


    private Set<String> generateServicesToBuildSet(Set<String> filesChanged){
        Set<String> serviceSet = ConcurrentHashMap.newKeySet();
        List<JenkinsJob> jenkinsJobList = this.jenkinsClient.describeJobs().getJobs();


        for(String filename : filesChanged){

            if(filename.contains("/")){
                String service = "";

                //Build services when infrastructure changes
                if(filename.startsWith("ops/terraform/")){
                    service = filename.split("/")[2];
                    serviceSet.add(service);
                }
                //Build services in the ops directory
                else if(filename.startsWith("ops/")){
                    service = filename.split("/")[1];
                    serviceSet.add(service);
                }
                //Build regular top-level services
                else {
                    service = filename.split("/")[0];
                    serviceSet.add(service);
                }

                // Check if the service matches a Jenkins Job
                final String serviceCheck = service;
                if(jenkinsJobList.stream().anyMatch(job -> job.getName().equalsIgnoreCase(serviceCheck))){
                    serviceSet.add(service);
                }
            }
        }
        return serviceSet;
    }

}
