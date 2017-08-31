package hooker.clients.jenkins;

import lombok.Data;

@Data
public class JenkinsJob {
    private String _class;
    private String name;
    private String url;

    public String getJenkinsClass(){
        return this._class;
    }
}