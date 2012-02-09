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



package utest;

import java.util.*;
import junit.framework.*;


public class TestUtil
{
    public static void runTests(Class testSuiteClass)
    {
        System.out.println("***** Test " + testSuiteClass.getName() + " *****");

        try
        {
            TestSuite   suite = new TestSuite(testSuiteClass);
            TestResult  result = new TestResult();
            suite.run(result);
            System.out.println("Tests: " + result.runCount() +
                               "  errors: " + result.errorCount() +
                               "  failures: " + result.failureCount());
            if (result.errorCount() > 0)
            {
                System.out.println("----- Test Errors -----");
                for (Enumeration i = result.errors(); i.hasMoreElements(); )
                {
                    TestFailure obj = (TestFailure)i.nextElement();
                    obj.thrownException().printStackTrace();
                }
            }
            if (result.failureCount() > 0)
            {
                System.out.println("----- Test Failures -----");
                for (Enumeration i = result.failures(); i.hasMoreElements(); )
                {
                    TestFailure obj = (TestFailure)i.nextElement();
                    obj.thrownException().printStackTrace();
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace(System.err);
        }
        
    }

}

