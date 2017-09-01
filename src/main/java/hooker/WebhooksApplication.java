package hooker;

import hooker.clients.github.GitHubClient;
import hooker.clients.jenkins.JenkinsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebhooksApplication {

	private Config config;

	public static void main(String[] args) {
		SpringApplication.run(WebhooksApplication.class, args);
	}

	@Autowired
	public WebhooksApplication(Config config) {
		this.config = config;
	}

	@Bean
	public GitHubClient gitHubClient() {
		return GitHubClient.builder()
				.githubUsername(this.config.getGithubUsername())
				.githubPersonalAccessToken(this.config.getGithubPersonalAccessToken())
				.repo(this.config.getRepoName())
				.owner(this.config.getRepoOwner())
				.build();
	}

	@Bean
	public JenkinsClient jenkinsClient() {
		return JenkinsClient.builder()
				.url(this.config.getJenkinsUrl())
				.apiToken(this.config.getJenkinAPIToken())
				.githubToken(this.config.getGithubToken())
				.user(this.config.getJenkinsUser())
				.build();
	}

}
