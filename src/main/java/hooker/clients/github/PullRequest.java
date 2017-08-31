package hooker.clients.github;

import lombok.Data;

@Data
public class PullRequest {

    private String id;
    private String state;
    private Long number;
    private String title;
    private String commits_url;
    private String url;
}
