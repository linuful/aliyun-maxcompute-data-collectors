/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.aliyun.odps.ogg.handler.datahub;

import com.aliyun.odps.ogg.handler.datahub.modle.Configure;
import com.aliyun.odps.ogg.handler.datahub.modle.PluginStatictics;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeoutHandler {
    private static Integer corePoolSize;
    private static Integer timeout;
    private static ScheduledExecutorService scheduled;
    private static TimeoutHandler timeoutHandler;
    private final static Logger logger;

    static {
        logger = LoggerFactory.getLogger(TimeoutHandler.class);
        timeout = 10000;
        corePoolSize = 3;
        timeoutHandler = new TimeoutHandler();
    }

    public static TimeoutHandler instance() {
        return timeoutHandler;
    }


    public static void init(Configure configure) {
        DataHubExecutor worker = new DataHubExecutor();

        timeout = configure.getTimeOut();

        if (null == scheduled) {
            scheduled = Executors.newScheduledThreadPool(corePoolSize);
            scheduled.scheduleAtFixedRate(worker, 1, timeout, TimeUnit.MILLISECONDS);
        }


    }

    public static void close() {
        scheduled.shutdown();
        boolean isDone = false;
        do {
            try {
                isDone = scheduled.awaitTermination(1, TimeUnit.DAYS);
            } catch (InterruptedException e) {
                logger.error(Throwables.getStackTraceAsString(e));
            }
            logger.warn("awaitTermination...");
        } while (!isDone);

        logger.info("DataHubExecutor is closed success!");
    }



    static class DataHubExecutor implements Runnable {
        //@SneakyThrows
        @Override
        public void run() {
            try {
                DataHubWriter.instance().flushAll();
                // save checkpoints
                HandlerInfoManager.instance().saveHandlerInfos();
            } catch (Exception e1) {
                logger.error("Unable to deliver records", e1);
            }
            reportStatus();
        }


        public void reportStatus() {
            StringBuilder sb = new StringBuilder();
            sb.append("transactions=").append(PluginStatictics.getTotalTxns());
            sb.append(", operations=").append(PluginStatictics.getTotalOperations());
            sb.append(", inserts=").append(PluginStatictics.getTotalInserts());
            sb.append(", updates=").append(PluginStatictics.getTotalUpdates());
            sb.append(", deletes=").append(PluginStatictics.getTotalDeletes());
            logger.info(sb.toString());
            //System.out.println(sb.toString());
        }
    }
}


