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


package wwutil.model.annotation;

import java.lang.annotation.*;


/** Model field annotation.  Declare a field to be stored in S3.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface S3Field {
    // Constants for storeAs
    public static final int     AS_JSON = 1;
    public static final int     AS_OBJECT = 2;

    public int storeAs() default AS_JSON;

    /** s3Bucket for accessing the S3 object.  Override the one in Jsoda. */
    public String s3Bucket() default "";

    /** s3KeyBase for accessing the S3 object.  Override the one in Jsoda. */
    public String s3KeyBase() default "";

    /** Compress the content with gzip before storing.  Set the Content-Encoding of the S3 object to gzip. */
    public boolean gzip() default false;

}

