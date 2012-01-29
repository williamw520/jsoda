
package wwutil.model;

import java.io.Serializable;

/**
 * Simple cache service interface.
 */
public interface MemCacheable {

    public Serializable get(String key);
    public void put(String key, int expireInSeconds, Serializable obj);
    public void delete(String key);
    public void resetStats();
    public int getHits();
    public int getMisses();
    public String dumpStats();

}

