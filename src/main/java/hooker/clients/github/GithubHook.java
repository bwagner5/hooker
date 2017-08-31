package hooker.clients.github;

import hooker.ServiceInfo;
import lombok.Data;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class GithubHook {

    private String ref;
    private boolean deleted;
    private List<Commit> commits;

    public String getBranch(){
        return this.getRef().replaceAll("refs/heads/", "");
    }

    public Map<String, ServiceInfo> generateServiceInfoMap(){
        Map<String, ServiceInfo> serviceInfoMap = new ConcurrentHashMap<>();

        this.commits.forEach(commit-> {
            Set<String> mods = ConcurrentHashMap.newKeySet();
            mods.addAll(commit.getAdded());
            mods.addAll(commit.getModified());
            mods.addAll(commit.getRemoved());

            mods.forEach(fileMod->{
                String service = fileMod.split("/")[0];
                if(serviceInfoMap.containsKey(service)){
                    serviceInfoMap.get(service).addToCommits(commit);
                }else{
                    serviceInfoMap.put(service, new ServiceInfo(service).addToCommits(commit));
                }
            });
        });

        return serviceInfoMap;
    }

    public Set<String> aggregateFileModifications(){
        Set<String> mods = ConcurrentHashMap.newKeySet();
        this.commits.forEach(commit-> {
                    mods.addAll(commit.getAdded());
                    mods.addAll(commit.getModified());
                    mods.addAll(commit.getRemoved());
                });
        return mods;
    }

}





