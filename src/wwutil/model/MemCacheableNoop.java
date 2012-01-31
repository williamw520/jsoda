
package wwutil.model;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.io.Serializable;



/**
 * A dummy cache service that does nothing.
 */
public class MemCacheableNoop implements MemCacheable {

    public Serializable get(String key) {
        return null;
    }

    public void put(String key, int expireInSeconds, Serializable obj) {
    }

    public void delete(String key) {
    }

    public void clearAll() {
    }

    public void shutdown() {
    }

    public void resetStats() {
    }

    public int getHits() {
        return 0;
    }

    public int getMisses() {
        return 0;
    }

    public String dumpStats() {
        int     hits = 0;
        int     misses = 0;
        int     total =  hits + misses;
        total = total == 0 ? 1 : total;
        return "total: " + total + "  hits: " + hits + " " + (hits*100/total) + "%  misses: " + misses + " " + (misses*100/total) + "%";
    }

}

