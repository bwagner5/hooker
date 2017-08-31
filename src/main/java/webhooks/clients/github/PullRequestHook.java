package webhooks.clients.github;

import lombok.Data;

import java.util.List;

@Data
public class PullRequestHook {

    private String action;
    private Long number;
    private PullRequest pull_request;

}
