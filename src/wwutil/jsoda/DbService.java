
package wwutil.jsoda;

import java.util.*;


interface DbService {
    public void createTable(String table);
    public void deleteTable(String table);
    public List<String> listTables();

}
