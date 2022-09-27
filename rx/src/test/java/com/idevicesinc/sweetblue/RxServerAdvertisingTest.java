/*

  Copyright 2022 Hubbell Incorporated

  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.

  You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.

 */

package com.idevicesinc.sweetblue;


import com.idevicesinc.sweetblue.rx.RxAdvertisingEvent;
import com.idevicesinc.sweetblue.rx.RxBleServer;
import com.idevicesinc.sweetblue.utils.BleScanRecord;
import com.idevicesinc.sweetblue.utils.Uuids;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.reactivestreams.Subscription;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.concurrent.atomic.AtomicInteger;
import io.reactivex.rxjava3.core.FlowableSubscriber;


@Config(manifest = Config.NONE, sdk = 25)
@RunWith(RobolectricTestRunner.class)
public final class RxServerAdvertisingTest extends RxBaseBleUnitTest
{

    @Test(timeout = 15000)
    public void startStopStartAdvertisingTest() throws Exception
    {
        m_config.loggingOptions = LogOptions.ON;

        final AtomicInteger startCount = new AtomicInteger(0);

        final BleScanRecord packet = new BleScanRecord(Uuids.BATTERY_SERVICE_UUID);

        m_manager.setConfig(m_config);

        final RxBleServer server = m_manager.getServer(null, null, null);

        // As we're stopping/starting, we need to observe advertising events (the startAdvertising method
        // returns a Single, which means we'd only get the callbacks for that method call, as it's an ephemeral listener)
        server.observeAdvertisingEvents().subscribe(new FlowableSubscriber<RxAdvertisingEvent>()
        {
            @Override public void onSubscribe(Subscription s)
            {
                s.request(Long.MAX_VALUE);
            }

            @Override public void onNext(RxAdvertisingEvent rxAdvertisingEvent)
            {
                if (startCount.get() > 3)
                {
                    server.stopAdvertising();
                    succeed();
                }

                assertTrue(rxAdvertisingEvent.wasSuccess());
                startCount.incrementAndGet();

                server.stopAdvertising();
                assertFalse(server.isAdvertising());

                server.startAdvertising(packet).subscribe();
            }

            @Override public void onError(Throwable t)
            {
            }

            @Override public void onComplete()
            {
            }
        });

        server.startAdvertising(packet).subscribe();

        startAsyncTest();
    }

}
