

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

