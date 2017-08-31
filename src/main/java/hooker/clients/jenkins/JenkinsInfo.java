package hooker.clients.jenkins;

import lombok.Data;

import java.util.List;

@Data
public class JenkinsInfo {
    private List<JenkinsJob> jobs;
}
