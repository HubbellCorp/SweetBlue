package com.idevicesinc.sweetblue.utils;


/**
 * Generic interface used when you need to specify the input type, and the output type
 * (hence FunctionIO)
 */
public interface FunctionIO<Input, Output>
{
    Output call(Input param);
}
