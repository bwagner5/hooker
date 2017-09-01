package hooker.clients.github;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import lombok.*;
import org.json.JSONArray;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@EqualsAndHashCode
@ToString(exclude = {"githubPersonalAccessToken", "githubUsername"})
@Builder
public class GitHubClient {

    private static final Logger LOGGER = Logger.getLogger( GitHubClient.class.getName() );

    private static final String URL = "https://api.github.com";

    @Getter @Setter
    private String owner;

    @Getter @Setter
    private String repo;

    @Setter
    private String githubUsername;

    @Setter
    private String githubPersonalAccessToken;


    public GitHubClient() {}

    public GitHubClient(String githubUsername, String githubPersonalAccessToken, String owner, String repo){
        this.githubUsername = githubUsername;
        this.githubPersonalAccessToken = githubPersonalAccessToken;
        this.owner = owner;
        this.repo = repo;
    }

    public Set<String> retrievePullRequestFileChanges(long pullRequestNumber){
        LOGGER.log(Level.INFO, "Requesting files changes for Pull Request: " + pullRequestNumber);
        Set<String> fileChanges = ConcurrentHashMap.newKeySet();
        HttpResponse<JsonNode> response = null;
        Set<String> pageChanges = ConcurrentHashMap.newKeySet();
        int currentPage = 0;
        do {
            try {
                response = Unirest.get(String.format("%s/repos/%s/%s/pulls/%s/files?page=%s", URL, this.owner, this.repo, String.valueOf(pullRequestNumber), String.valueOf(++currentPage)))
                        .basicAuth(this.githubUsername, this.githubPersonalAccessToken)
                        .asJson();
            } catch(UnirestException ue){
                LOGGER.log(Level.SEVERE, "There was an error when requesting data from the Pull Request Number: " + pullRequestNumber);
                return null;
            }
            if (response != null && response.getStatus() >= 200 && response.getStatus() < 300) {
                pageChanges = this.jsonArrayToStringSet(response.getBody().getArray(), "filename");
                fileChanges.addAll(pageChanges);
            } else {
                LOGGER.log(Level.SEVERE, "Could not retrieve Pull Request File Changes for PR: " + pullRequestNumber + " : " + response.getStatusText());
            }
            LOGGER.log(Level.INFO, String.format("Fetched %d Files for Page %d", pageChanges.size(), currentPage));
        }while(!pageChanges.isEmpty());

        return fileChanges;
    }



    private Set<String> jsonArrayToStringSet(JSONArray jsonArray, String key){
        Set<String> fileChangeSet = ConcurrentHashMap.newKeySet();
        for(int i=0; i<jsonArray.length(); i++){
            fileChangeSet.add(jsonArray.getJSONObject(i).getString(key));
        }
        return fileChangeSet;
    }


}
