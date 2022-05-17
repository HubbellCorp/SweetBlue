package com.idevicesinc.sweetblue.toolbox.util;

import java.io.File;


public final class ExportResult
{
    private boolean success;
    private String filename;
    private File file;


    public void setSuccess(boolean success)
    {
        this.success = success;
    }

    public void setFilename(String filename)
    {
        this.filename = filename;
    }

    public void setFile(File file)
    {
        this.file = file;
    }

    public boolean wasSuccess()
    {
        return success;
    }

    public String fileName()
    {
        return filename;
    }

    public File file()
    {
        return file;
    }
}
