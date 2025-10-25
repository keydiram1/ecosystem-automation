package api.backup;

import api.backup.dto.RetrieveEntityRecord;
import com.aerospike.client.Key;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import utils.ASBench;
import utils.AerospikeLogger;
import utils.AutoUtils;
import utils.aerospike.adr.AerospikeDataUtils;
import utils.init.runners.BackupRunner;
import utils.prometheus.PrometheusReader;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;

@Disabled
class LongRunningTests extends BackupRunner {
    private static final String SET_NAME = "testset";
    private static final String SOURCE_NAMESPACE = "source-ns1";
    private static final String SOURCE_CLUSTER_NAME = "srcCluster1";
    private static final Key key = new Key(SOURCE_NAMESPACE, SET_NAME, "IT");

    private final PrometheusReader prometheusReader = new PrometheusReader();

    @Test
    void highLoad() {
        final long backupRecordsInitial = prometheusReader.readQueueHandlerMetrics().backup_records_total;
        int duration = 20;

        ASBench.on(SOURCE_NAMESPACE, SET_NAME).duration(duration).port(3000).run();

        long backupTotal = 0;
        long backupPrev;
        do {
            backupPrev = backupTotal;
            AutoUtils.sleep(15_000);
            backupTotal = prometheusReader.readQueueHandlerMetrics().backup_records_total - backupRecordsInitial;
            AerospikeLogger.info("Backed up " + backupTotal);
        } while (backupTotal - backupPrev > 0);
    }

    @Test
    void retrieveHL() throws JsonProcessingException {
        int delay = 120_000;
        long start = System.currentTimeMillis();

        ScheduledFuture<?> generator = newSingleThreadScheduledExecutor().scheduleAtFixedRate(
                () -> AerospikeDataUtils.put(key, "time", System.currentTimeMillis()), 0, 10, TimeUnit.MILLISECONDS
        );

        AutoUtils.sleep(delay);
        generator.cancel(true);

        long stop = System.currentTimeMillis();

        Map<Long, Long> timeToDelta = Stream.iterate(start, t -> t < stop, t -> t + 100)
                .collect(Collectors.toMap(
                        t -> t - start,
                        t -> t - getBackedUpTimestamp(t),
                        (e1, e2) -> e1, TreeMap::new));

        System.out.println(new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(timeToDelta));
    }

    private static long getBackedUpTimestamp(long initialTimeStamp) {
        String digest = AerospikeDataUtils.getDigestFromKey(key).toUpperCase();
        List<RetrieveEntityRecord> retrieve = RetrieveAPI.retrieve(RetrieveAPI.WhatToRetrieve.LATEST, initialTimeStamp, digest, SOURCE_CLUSTER_NAME, SOURCE_NAMESPACE, SET_NAME);
        return retrieve.get(0).getTimestamp();
    }
}
