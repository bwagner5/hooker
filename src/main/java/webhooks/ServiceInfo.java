package webhooks;

import webhooks.clients.github.Commit;
import lombok.Data;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class ServiceInfo {
    private String service;
    private Set<Commit> commits;

    public ServiceInfo() { }

    public ServiceInfo(String service) {
        this.service = service;
        this.commits = ConcurrentHashMap.newKeySet();
    }

    public ServiceInfo addToCommits(Commit commit){
        this.commits.add(commit);
        return this;
    }

    public void filterCommitChangesets(){
        this.getCommits().forEach(commit->{
            Set<String> mods = ConcurrentHashMap.newKeySet();
            if(commit.getAdded() != null){
                mods.addAll(commit.getAdded());
            }
            if(commit.getModified() != null){
                mods.addAll(commit.getModified());
            }
            if(commit.getRemoved() != null){
                mods.addAll(commit.getRemoved());
            }

            mods.forEach(dir->{
                String service = dir.split("/")[0];
                if(!service.equals(this.service)){
                    mods.remove(dir);
                }
            });

            commit.setModified(mods);
        });
    }
}
