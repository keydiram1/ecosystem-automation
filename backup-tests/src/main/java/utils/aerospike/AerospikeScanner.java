package utils.aerospike;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.IAerospikeClient;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.ScanCallback;
import com.aerospike.client.policy.ScanPolicy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AerospikeScanner implements ScanCallback {

    private Map<String, Metrics> setMap = new HashMap<>();

    private final List<Key> allKeys = new ArrayList<>();
    private Key randomKey;
    private long count;

    public void scan(IAerospikeClient client, String namespace, String setName) {
        allKeys.clear();
        setMap = new HashMap<>();
        List<String> nodeList = client.getNodeNames();

        for (String nodeName : nodeList) {
            client.scanNode(new ScanPolicy(), nodeName, namespace, setName, this);

            for (Map.Entry<String, Metrics> entry : setMap.entrySet()) {
                entry.getValue().count = 0;
            }
        }
        if (setMap.get(setName) == null) {
            count = 0;
        } else {
            count = setMap.get(setName).total;
        }
    }

    public List<Key> scanKeys(IAerospikeClient client, String namespace, String setName) {
        allKeys.clear();
        setMap = new HashMap<>();
        List<String> nodeList = client.getNodeNames();

        for (String nodeName : nodeList) {
            ScanPolicy scanPolicy = new ScanPolicy();
            scanPolicy.includeBinData = false;

            client.scanNode(scanPolicy, nodeName, namespace, setName, (key, record) -> {
                if (key.userKey != null) {
                    allKeys.add(key);
                }
            });
        }

        return allKeys;
    }

    public List<Key> getAllKeys(){
        return allKeys;
    }

    public Key getRandomKey(){
        return randomKey;
    }

    public long getCount(){
        return count;
    }

    @Override
    public void scanCallback(Key key, Record record) throws AerospikeException {
        Metrics metrics = setMap.get(key.setName);

        if (metrics == null) {
            metrics = new Metrics();
        }
        metrics.count++;
        metrics.total++;
        setMap.put(key.setName, metrics);
        // might want to create a list of key and wait for backup for all of them
        randomKey = key;
        allKeys.add(key);
    }

    private static class Metrics {
        public long count = 0;
        public long total = 0;
    }

    public Map<String, Metrics> getSetMap() {
        return setMap;
    }
}
