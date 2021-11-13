package click.alchemist.cook.service.couchbase;

import com.couchbase.lite.ReplicatorConfiguration;

/**
 * Java Helper to fix static class access visibility not from base class in java through kotlin.
 */
public class ReplicatorTypeHelper {
    public static ReplicatorConfiguration.ReplicatorType getReplicatorTypeFor(Boolean pull, Boolean push) {
        if (pull && push) return ReplicatorConfiguration.ReplicatorType.PUSH_AND_PULL;
        if (pull) return ReplicatorConfiguration.ReplicatorType.PULL;
        if (push) return ReplicatorConfiguration.ReplicatorType.PUSH;
        return null;
    }
}