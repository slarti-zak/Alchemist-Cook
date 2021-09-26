package click.alchemist.cook.service.couchbase;

import com.couchbase.lite.ReplicatorConfiguration;

public class ReplicatorTypeHelper {
    public static ReplicatorConfiguration.ReplicatorType getReplicatorTypeFor(Boolean pull, Boolean push) {
        if (pull && push) return ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL;
        if (pull) return ReplicatorConfiguration.ReplicatorType.PULL;
        if (push) return ReplicatorConfiguration.ReplicatorType.PUSH;
        return null;
    }
}