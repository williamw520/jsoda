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

package wwutil.sys;


import java.io.*;
import java.util.*;
import java.util.zip.*;



public class IOUtil {

    public static void close(OutputStream os) {
        if (os != null) {
            try {
                os.close();
            } catch (Exception ignored) { }
        }
    }

    public static void close(InputStream is) {
        if (is != null) {
            try {
                is.close();
            } catch (Exception ignored) { }
        }
    }

    public static void close(Writer os)
    {
        if (os != null) {
            try {
                os.close();
            } catch (Exception ignored) { }
        }
    }

    public static void close(Reader is)
    {
        if (is != null) {
            try {
                is.close();
            } catch (Exception ignored) { }
        }
    }

    public static void deleteAllFiles(File directory) {

        File[] files = directory.listFiles();
        for (File file : files) {
            if (!file.delete()) {
                // System.out.println("Cannot delete " + file);
            }
        }
    }

    public static String readString(InputStream input, String encoding)
        throws IOException
    {
        InputStreamReader   ir = (encoding == null ? new InputStreamReader(input) : new InputStreamReader(input, encoding));
        StringWriter        sw = new StringWriter();
        try {
            copy(ir, sw);
            return sw.toString();
        } finally {
            close(sw);
        }
    }    

    public static long copy(InputStream in, OutputStream out)
        throws IOException
    {
        byte[]  buffer = new byte[1024 * 4];
        int     len;
        long    count = 0;

        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
            count += len;
        }
        return count;
    }

    public static long copy(Reader in, Writer out)
        throws IOException
    {
        char[]  buffer = new char[1024 * 4];
        int     len;
        long    count = 0;

        while ((len = in.read(buffer)) >= 0) {
            out.write(buffer, 0, len);
            count += len;
        }
        return count;
    }

    public static long copy(File src, File dest)
        throws IOException
    {
        FileInputStream		fis = new FileInputStream(src);
        FileOutputStream	fos = new FileOutputStream(dest);
        try {
            return copy(fis, fos);
        } finally {
            close(fis);
            close(fos);
        }
    }

    public static byte[] objToBytes(Serializable obj)
        throws IOException
    {
        ByteArrayOutputStream   bos = new ByteArrayOutputStream();
        ObjectOutputStream      oo = new ObjectOutputStream(bos);
        try {
            oo.writeObject(obj);
            return bos.toByteArray();
        } finally {
            close(oo);
            close(bos);
        }
    }

    public static Object objFromBytes(byte[] bytes)
        throws IOException, ClassNotFoundException
    {
        ByteArrayInputStream    bis = new ByteArrayInputStream(bytes);
        ObjectInputStream       oi = new ObjectInputStream(bis);
        try {
            return oi.readObject();
        } finally {
            close(oi);
            close(bis);
        }
    }    

    public static void objToStream(OutputStream os, Serializable obj)
        throws IOException
    {
        ObjectOutputStream  oo = new ObjectOutputStream(os);
        try {
            oo.writeObject(obj);
        } finally {
            close(oo);
        }
    }

    public static Object objFromStream(InputStream is)
        throws IOException, ClassNotFoundException
    {
        ObjectInputStream       oi = new ObjectInputStream(is);
        try {
            return oi.readObject();
        } finally {
            close(oi);
        }
    }    

    public static void saveObject(File file, Serializable obj)
        throws IOException
    {
        objToStream(new BufferedOutputStream(new FileOutputStream(file)), obj);
    }

    public static Object loadObject(File file)
        throws IOException, ClassNotFoundException
    {
        return objFromStream(new BufferedInputStream(new FileInputStream(file)));
    }
}

