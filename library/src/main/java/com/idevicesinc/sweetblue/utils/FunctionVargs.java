package com.idevicesinc.sweetblue.utils;

public interface FunctionVargs<Output>
{
    Output call(Vargs args);

    final class Vargs {
        final Object[] m_args;

        public Vargs(Object... args)
        {
            m_args = args;
        }

        @SuppressWarnings("unchecked")
        public <T> T get(int index)
        {
            if (m_args != null && index >= 0 && index < m_args.length)
                return (T) m_args[index];
            return null;
        }
    }
}
