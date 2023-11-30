package com.idevicesinc.sweetblue.framework;

import android.app.Activity;
import android.widget.EditText;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import org.junit.After;
import org.junit.Before;

import java.io.File;

/**
 * Base test class to extend for using UIAutomator for writing instrumented android tests.
 * Note that a {@link UiDevice} will already be instantiated, as mDevice, and a {@link UiSelector} as well, as mUiSelector.
 */
public abstract class BaseUiAutomatorTest<T extends Activity> extends AbstractTestClass
{

    protected static int EDIT_TEXT_TIMEOUT = 5000;

    protected UiDevice mDevice;
    protected UiSelector mUiSelector;


    @Before
    public final void setup() throws Exception
    {
        // Initialize UiDevice instance
        mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

        mUiSelector = new UiSelector();

        postSetup();
    }

    /**
     * Override this method if you need to do any additional setup operations before the first activity is instantiated
     */
    protected void preSetup()
    {
        // No-op
    }

    /**
     * Override this method if you need to do any additional setup operations after the first activity is instantiated
     */
    protected void postSetup()
    {
        // No-op
    }

    /**
     * Override this method if you need to do anything additional after every test is run.
     */
    protected void additionalTearDown()
    {
        // No-op
    }

    /**
     * The default option is <code>true</code>, in that after each individual test is run, the app's data will be cleared. If you
     * do NOT want this to happen, override this method, and return <code>false</code>.
     */
    protected boolean clearDataAfterEveryTest()
    {
        return true;
    }

    /**
     * Set the text input for an {@link EditText} that is on the screen. The instance number refers to the "instance" number
     * of the view. This means if there are 4 on the screen, and you put instance 1, you will update the 2nd {@link EditText}
     * that is on the screen in the view hierarchy. This has a built in timeout value which is dictated by
     * {@link #EDIT_TEXT_TIMEOUT}. It's a static field, so change it if you feel you want more or less of a timeout.
     *
     * @return <code>true</code> if there was an error, <code>false</code> if the call worked
     */
    public boolean editTextInput(int instance, String input) // false = no error
    {
        UiObject userInput = mDevice.findObject(mUiSelector.instance(instance).className(EditText.class));

        if (userInput.exists())
        {
            userInput.waitForExists(EDIT_TEXT_TIMEOUT);
            try
            {
                userInput.setText(input);
            } catch (UiObjectNotFoundException e)
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Like {@link #deleteFile(File)}, only any directories with the name provided will NOT be
     * deleted.
     */
    public static boolean deleteFile(File file, String exemptDirName)
    {
        boolean success = true;
        if (file != null)
            if (file.isDirectory())
            {
                File[] files = file.listFiles();
                for (int i = 0; i < files.length; i++)
                {
                    File f = files[i];
                    if (f.isDirectory())
                    {
                        if (!f.getName().equals(exemptDirName))
                        {
                            success = deleteFile(f);
                        }
                        else
                        {
                            success = true;
                        }
                    }
                    else
                    {
                        success = f.delete();
                    }
                    if (!success)
                    {
                        return false;
                    }
                }
            }
            else
            {
                success = file.delete();
            }
        return success;
    }

    /**
     * Delete the file/directory recursively. If you give this method a file, it will be deleted, and if you
     * give it a directory, the directory's contents will be deleted, and the dir removed.
     */
    public static boolean deleteFile(File dir)
    {
        boolean success = true;
        if (dir != null)
        {
            if (dir.isDirectory())
            {
                File[] files = dir.listFiles();
                for (int i = 0; i < files.length; i++)
                {
                    File f = files[i];
                    if (f.isDirectory())
                    {
                        success = deleteFile(f);
                    }
                    else
                    {
                        success = f.delete();
                    }
                    if (!success)
                    {
                        return false;
                    }
                }
            }
            else
            {
                success = dir.delete();
            }
        }
        return success;
    }

    /**
     * This method is called after each test has run. If you need to perform additional operations, then override
     * {@link #additionalTearDown()} instead (which gets called first by this method).
     */
    @After
    public final void tearDown() throws Exception
    {
        additionalTearDown();
        mDevice = null;
        mUiSelector = null;
    }

}
