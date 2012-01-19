
package wwutil.model.annotation;

import java.lang.annotation.*;


/** Model class annotation.  General model information for generated UI.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AInfo{
    public String baseUrl();
    public String listTitle();
    public String listDesc();
    public String itemTitle();
    public String itemDesc();
}
