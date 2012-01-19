
package wwutil.db;

import java.io.Serializable;

/**
 * Simple cache service interface.
 */
public interface MemCacheable {

    public Serializable get(String key);
    public void put(String key, int expireInSeconds, Serializable obj);
    public void delete(String key);
    public void resetStats();
    public long getHits();
    public long getMisses();
    public String dumpStats();

}

