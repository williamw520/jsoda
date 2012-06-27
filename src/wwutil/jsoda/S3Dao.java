
package wwutil.jsoda;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;
import java.lang.reflect.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.ObjectMetadata;

import wwutil.sys.ReflectUtil;
import wwutil.sys.IOUtil;
import wwutil.model.annotation.S3Field;



public class S3Dao<T> {

    private static Log  log = LogFactory.getLog(S3Dao.class);

    private Class<T>    modelClass;
    private String      modelName;
    private Jsoda       jsoda;


    public S3Dao(Class<T> modelClass, Jsoda jsoda) {
        this.modelClass = modelClass;
        this.modelName = jsoda.getModelName(modelClass);
        this.jsoda = jsoda;
    }


    void saveS3Fields(T dataObj)
        throws Exception
    {
        for (Field field : jsoda.getS3Fields(modelName).values()) {
            Object  value = field.get(dataObj);
            if (value == null)
                continue;

            boolean gzip = ReflectUtil.getAnnotationValueEx(field, S3Field.class, "gzip", boolean.class, Boolean.FALSE);

            switch (ReflectUtil.getAnnotationValueEx(field, S3Field.class, "storeAs", int.class, S3Field.AS_JSON)) {
            case S3Field.AS_JSON: {
                S3Dao.uploadJsonToS3(jsoda.getS3Client(), getS3Bucket(field), formatS3Key(dataObj, field), value, gzip);
                break;
            }
            case S3Field.AS_OBJECT: {
                S3Dao.uploadObjectToS3(jsoda.getS3Client(), getS3Bucket(field), formatS3Key(dataObj, field), (Serializable)value, gzip);
                break;
            }
            }
        }        
    }

    void loadS3Fields(T dataObj)
        throws JsodaException
    {
        Map<String, Field> s3Fields = jsoda.getS3Fields(modelName);

        for (Field field : jsoda.getS3Fields(modelName).values()) {
            try {
                boolean gzip = ReflectUtil.getAnnotationValueEx(field, S3Field.class, "gzip", boolean.class, Boolean.FALSE);
                Object  value = null;
                switch (ReflectUtil.getAnnotationValueEx(field, S3Field.class, "storeAs", int.class, S3Field.AS_JSON)) {
                case S3Field.AS_JSON: {
                    value = S3Dao.downloadJsonFromS3(jsoda.getS3Client(), getS3Bucket(field), formatS3Key(dataObj, field), field.getType(), gzip);
                    break;
                }
                case S3Field.AS_OBJECT: {
                    value = S3Dao.downloadObjectFromS3(jsoda.getS3Client(), getS3Bucket(field), formatS3Key(dataObj, field), gzip);
                    break;
                }
                }

                if (value == null)
                    continue;
                field.set(dataObj, value);
            } catch(Exception e) {
                throw new JsodaException("Failed to load S3Field " + field.getName(), e);
            }
        }        

    }

    void deleteS3Fields(Object id, Object rangeKey)
        throws JsodaException
    {
        Map<String, Field> s3Fields = jsoda.getS3Fields(modelName);

        for (Field field : jsoda.getS3Fields(modelName).values()) {
            try {
                jsoda.getS3Client().deleteObject(getS3Bucket(field), formatS3Key(id, rangeKey, field));
            } catch(Exception e) {
                throw new JsodaException("Failed to delete S3Field " + field.getName(), e);
            }
        }        

    }

    private String getS3Bucket(Field field) {
        // Get s3Bucket from the S3Field with backup default from the Jsoda object.
        return ReflectUtil.getAnnotationValue(field, S3Field.class, "s3Bucket", jsoda.getDefaultS3Bucket());
    }

    private String formatS3Key(Object idKey, Object rangeKey, Field field)
        throws java.lang.IllegalAccessException
    {
        String  globalKeyPrefix = (!StringUtils.isEmpty(jsoda.getS3KeyPrefix()) ? jsoda.getS3KeyPrefix() : "");
        String  keyBase = ReflectUtil.getAnnotationValue(field, S3Field.class, "s3KeyBase", modelName);
        String  objectKey = jsoda.makePkKey(modelName, idKey, rangeKey);
        String  fieldName = field.getName();

        return globalKeyPrefix + keyBase + "/" + objectKey + "/" + fieldName;
    }

    private String formatS3Key(T dataObj, Field field)
        throws java.lang.IllegalAccessException
    {
        Field   idField = jsoda.getIdField(modelName);
        Object  idKey = idField.get(dataObj);
        Field   rangeField = jsoda.getRangeField(modelName);
        Object  rangeKey = rangeField == null ? null : rangeField.get(dataObj);
        return formatS3Key(idKey, rangeKey, field);
    }

    public String formatS3Key(T dataObj, String fieldName)
        throws java.lang.IllegalAccessException
    {
        Field   field = jsoda.getS3Fields(modelName).get(fieldName);

        if (field != null)
            return formatS3Key(dataObj, field);
        else
            throw new IllegalArgumentException("Field " + fieldName + " is not a @S3Field.");
    }


    public static long uploadStreamToS3(AmazonS3Client s3, String s3bucket, String s3key, InputStream is, String contentType, long contentLength, String contentEncoding)
        throws IOException
    {
        ObjectMetadata  md = new ObjectMetadata();
        if (contentType != null)
            md.setContentType(contentType);
        md.setContentLength(contentLength);
        if (contentEncoding != null)
            md.setContentEncoding(contentEncoding);
        s3.putObject(s3bucket, s3key, is, md);
        return contentLength;
    }

    public static long uploadFileToS3(AmazonS3Client s3, String s3bucket, String s3key, File localFile, String contentType)
        throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(new FileInputStream(localFile));
        try {
            return uploadStreamToS3(s3, s3bucket, s3key, bis, contentType, localFile.length(), null);
        } finally {
            IOUtil.close(bis);
        }
    }

    public static long uploadBytesToS3(AmazonS3Client s3, String s3bucket, String s3key, byte[] bytes, String contentType, String contentEncoding)
        throws IOException
    {
        BufferedInputStream bis = new BufferedInputStream(new ByteArrayInputStream(bytes));
        try {
            return uploadStreamToS3(s3, s3bucket, s3key, bis, contentType, bytes.length, contentEncoding);
        } finally {
            IOUtil.close(bis);
        }
    }

    public static long uploadGzipBytesToS3(AmazonS3Client s3, String s3bucket, String s3key, byte[] bytes, String contentType, boolean gzip)
        throws IOException
    {
        if (gzip) {
            // Compress data
            ByteArrayOutputStream   bosZipped = new ByteArrayOutputStream();
            GZIPOutputStream        zos = new GZIPOutputStream(bosZipped);
            try {
                zos.write(bytes);
                IOUtil.close(zos);
                zos = null;
                return uploadBytesToS3(s3, s3bucket, s3key, bosZipped.toByteArray(), contentType, "gzip");
            } finally {
                IOUtil.close(zos);
            }
        } else {
            // Plain data
            return uploadBytesToS3(s3, s3bucket, s3key, bytes, contentType, null);
        }
    }

    public static long uploadJsonToS3(AmazonS3Client s3, String s3bucket, String s3key, Object obj, boolean gzip)
        throws IOException
    {
        String  json = DataUtil.toJson(obj);
        return uploadGzipBytesToS3(s3, s3bucket, s3key, json.getBytes("UTF-8"), "application/json; charset=UTF-8", gzip);
    }

    public static long uploadObjectToS3(AmazonS3Client s3, String s3bucket, String s3key, Serializable obj, boolean gzip)
        throws IOException
    {
        byte[]  bytes = IOUtil.objToBytes(obj);
        return uploadGzipBytesToS3(s3, s3bucket, s3key, bytes, "application/octet-stream", gzip);
    }

    public static long uploadStrToS3(AmazonS3Client s3, String s3bucket, String s3key, String str, boolean gzip)
        throws IOException
    {
        return uploadGzipBytesToS3(s3, s3bucket, s3key, str.getBytes("UTF-8"), "text/plain; charset=UTF-8", gzip);
    }


    public static S3Object getS3Object(AmazonS3Client s3, String s3bucket, String s3key) {
        return s3.getObject(s3bucket, s3key);
    }

    public static InputStream downloadStreamFromS3(AmazonS3Client s3, String s3bucket, String s3key)
        throws IOException
    {
        return getS3Object(s3, s3bucket, s3key).getObjectContent();
    }

    public static byte[] downloadBytesFromS3(AmazonS3Client s3, String s3bucket, String s3key, boolean gzip)
        throws IOException
    {
        InputStream             is = downloadStreamFromS3(s3, s3bucket, s3key);
        ByteArrayOutputStream   bos = new ByteArrayOutputStream();
        GZIPInputStream         gis = null;

        try {
            if (gzip) {
                gis = new GZIPInputStream(is);
                IOUtil.copy(gis, bos);
            } else {
                IOUtil.copy(is, bos);
            }
            return bos.toByteArray();
        } finally {
            IOUtil.close(gis);
            IOUtil.close(bos);
            IOUtil.close(is);
        }
    }

    public static String downloadStrFromS3(AmazonS3Client s3, String s3bucket, String s3key, boolean gzip)
        throws IOException
    {
        byte[]  bytes = downloadBytesFromS3(s3, s3bucket, s3key, gzip);
        return new String(bytes, "UTF-8");
    }

    public static <T> T downloadJsonFromS3(AmazonS3Client s3, String s3bucket, String s3key, Class<T> objClass, boolean gzip)
        throws Exception
    {
        String  json = downloadStrFromS3(s3, s3bucket, s3key, gzip);
        return DataUtil.fromJson(json, objClass);
    }

    public static Object downloadObjectFromS3(AmazonS3Client s3, String s3bucket, String s3key, boolean gzip)
        throws Exception
    {
        byte[]  bytes = downloadBytesFromS3(s3, s3bucket, s3key, gzip);
        return IOUtil.objFromBytes(bytes);
    }


}

