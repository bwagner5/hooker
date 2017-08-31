package hooker.clients.github;

import lombok.Data;

@Data
public class PullRequestHook {

    private String action;
    private Long number;
    private PullRequest pull_request;

}
