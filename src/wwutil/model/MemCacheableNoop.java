/******************************************************************************
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0.  If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 * 
 * Software distributed under the License is distributed on an "AS IS" basis, 
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for 
 * the specific language governing rights and limitations under the License.
 *
 * The Original Code is: Jsoda
 * The Initial Developer of the Original Code is: William Wong (williamw520@gmail.com)
 * Portions created by William Wong are Copyright (C) 2012 William Wong, All Rights Reserved.
 *
 ******************************************************************************/


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

