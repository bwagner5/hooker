package webhooks.clients.github;

import lombok.Data;

import java.util.Set;

@Data
public class Commit {

    private String id;
    private String message;
    //	private LocalDateTime timestamp;
    private Committer committer;

    private boolean distinct;
    private Set<String> added;
    private Set<String> removed;
    private Set<String> modified;

    public Commit() { }

    public Commit(String id, String message, Committer committer, boolean distinct, Set<String> added, Set<String> removed, Set<String> modified){
        this.id = id;
        this.message = message;
        this.committer = committer;
        this.distinct = distinct;
        this.added = added;
        this.removed = removed;
        this.modified = modified;
    }
}