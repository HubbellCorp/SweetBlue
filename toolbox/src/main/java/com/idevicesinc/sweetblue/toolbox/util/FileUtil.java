package com.idevicesinc.sweetblue.toolbox.util;


import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class FileUtil
{

    private FileUtil() {}


    /**
     * Implementation of {@link IFileUtil} interface used when calling the public static methods in this class. When unit testing, provide a new instance of the
     * interface here to allow for easier testing.
     */
    public static IFileUtil s_utilsImpl = new FileUtilImpl();


    /**
     * Writes the given {@code contents} to the {@code fileName} given, if it doesn't exist. If it does, then it will keep incrementing
     * the number attached to it. This assumes you are passing in a fileName string with a %s in it to append the number.
     */
    public static ExportResult writeUniqueFile(String fileNameFormat, String contents)
    {
        return s_utilsImpl.writeUniqueFile(fileNameFormat, contents);
    }

    /**
     * Reads a file and outputs JSON from the given {@link Uri}.
     */
    public static JSONObject readJSONFromUri(Context context, Uri uri) throws IOException, JSONException
    {
        return s_utilsImpl.readJSONFromUri(context, uri);
    }



    /**
     * Interface which does the actual logic of the methods that are exposed in {@link FileUtil}
     */
    public interface IFileUtil
    {
        ExportResult writeUniqueFile(String fileNameFormat, String contents);
        JSONObject readJSONFromUri(Context context, Uri uri) throws IOException, JSONException;
    }

    /**
     * Default implementation of {@link IFileUtil} which is used by the app by default. This would only need to change if the app is being
     * unit tested (instrumentation tests should still use this class, unless there's a glaring need to change this behavior).
     */
    public static class FileUtilImpl implements IFileUtil
    {

        @Override
        public ExportResult writeUniqueFile(String fileNameFormat, String contents)
        {
            ExportResult result = new ExportResult();

            File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            Environment.getDownloadCacheDirectory();

            // Look for a file name that isn't taken
            String filename = String.format(fileNameFormat, "");
            File file = null;

            int counter = 1;
            do
            {
                file = new File(path, filename);
                if (!file.exists())
                    break;
                file = null;
                filename = String.format(fileNameFormat, ++counter);
            } while (true && counter < 100);  // Limit is just to avoid an insanely long loop if we somehow can't find an suitable file

            if (file == null)
            {
                return result;
            }

            path.mkdirs();

            try
            {
                OutputStream os = new FileOutputStream(file);
                byte[] data = contents.getBytes("US-ASCII");
                os.write(data);
                os.close();
            }
            catch (Exception e)
            {
                return result;
            }

            result.setFile(file);
            result.setFilename(filename);
            result.setSuccess(true);

            return result;
        }

        @Override
        public JSONObject readJSONFromUri(Context context, Uri uri) throws IOException, JSONException
        {
            ContentResolver cr = context.getContentResolver();
            InputStream is = cr.openInputStream(uri);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            final int kBufferSize = 1024;
            byte buf[] = new byte[kBufferSize];
            int len;
            while ((len = is.read(buf)) > 0)
            {
                os.write(buf, 0, len);
            }

            String JSONString = os.toString();
            JSONObject jo = new JSONObject(JSONString);

            return jo;
        }
    }

}
