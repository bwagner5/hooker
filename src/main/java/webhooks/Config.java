package webhooks;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@Data
public class Config {

    @Value("${githubToken}")
    private String githubToken;

    @Value("${jenkinsUser}")
    private String jenkinsUser;

    @Value("${jenkinsAPIToken}")
    private String jenkinAPIToken;

    @Value("${jenkinsUrl}")
    private String jenkinsUrl;

    @Value("${githubUsername}")
    private String githubUsername;

    @Value("${githubPersonalAccessToken}")
    private String githubPersonalAccessToken;

    @Value("${privateDns}")
    private String privateDns;

    @Value("${s3HookInfoBucket}")
    private String s3HookInfoBucket;

    @Value("${repoOwner}")
    private String repoOwner;

    @Value("${repoName}")
    private String repoName;

    @Value("${emailNotificationFrom}")
    private String emailNotificationFrom;

    @Value("${blackListDomain}")
    private String blackListDomain;

}
