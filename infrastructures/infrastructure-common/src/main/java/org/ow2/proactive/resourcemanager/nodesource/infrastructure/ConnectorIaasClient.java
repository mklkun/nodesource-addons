package org.ow2.proactive.resourcemanager.nodesource.infrastructure;

import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.common.collect.Sets;


public class ConnectorIaasClient {

    private static final Logger logger = Logger.getLogger(ConnectorIaasClient.class);

    private static final int MAX_RETRIES_IN_CASE_OF_ERROR = 50;
    private static final int SLEEP_TIME_RETRIES_IN_CASE_OF_ERROR = 5000;

    private final RestClient restClient;

    public static RestClient generateRestClient(String connectorIaasURL) {
        return new RestClient(connectorIaasURL);
    }

    public ConnectorIaasClient(RestClient restClient) {
        this.restClient = restClient;
    }

    public void waitForConnectorIaasToBeUP() {
        int count = 0;
        while (true) {
            try {
                restClient.getInfrastructures();
                return;
            } catch (Exception e) {
                if (++count == MAX_RETRIES_IN_CASE_OF_ERROR) {
                    logger.error(e);
                    throw e;
                } else {
                    sleepFor(SLEEP_TIME_RETRIES_IN_CASE_OF_ERROR);
                }
            }
        }

    }

    public Set<JSONObject> getAllJsonInstancesByInfrastructureId(String infrastructureId) {
        Set<JSONObject> existingInstances = Sets.newHashSet();

        JSONArray instancesJSONObjects = new JSONArray(
            restClient.getInstancesByInfrastructure(infrastructureId));

        Iterator<Object> instancesJSONObjectsIterator = instancesJSONObjects.iterator();

        while (instancesJSONObjectsIterator.hasNext()) {
            existingInstances.add(((JSONObject) instancesJSONObjectsIterator.next()));
        }

        return existingInstances;
    }

    public String createInfrastructure(String infrastructureId, String infrastructureJson) {
        terminateInfrastructure(infrastructureId);
        return restClient.postToInfrastructuresWebResource(infrastructureJson);
    }

    public Set<String> createInstancesIfNotExisist(String infrastructureId, String instanceTag,
            String instanceJson, Set<JSONObject> existingInstances) {
        Set<String> instancesIds = getExistingInstanceIds(infrastructureId, instanceTag, existingInstances);

        if (instancesIds.isEmpty()) {
            instancesIds = createInstances(infrastructureId, instanceJson);
        }

        return instancesIds;
    }

    private Set<String> getExistingInstanceIds(String infrastructureId, String instanceTag,
            Set<JSONObject> existingInstances) {
        Set<String> instancesIds = Sets.newHashSet();

        for (JSONObject instance : existingInstances) {
            if (instance.getString("tag").equals(instanceTag)) {
                instancesIds.add(instance.getString("id"));
            }
        }

        return instancesIds;
    }

    private Set<String> createInstances(String infrastructureId, String instanceJson) {
        String response = restClient.postToInstancesWebResource(infrastructureId, instanceJson);

        JSONArray instancesJSONObjects = new JSONArray(response);

        Set<String> instancesIds = Sets.newHashSet();

        Iterator<Object> instancesJSONObjectsIterator = instancesJSONObjects.iterator();

        while (instancesJSONObjectsIterator.hasNext()) {
            instancesIds.add(((JSONObject) instancesJSONObjectsIterator.next()).getString("id"));
        }

        return instancesIds;
    }

    public void terminateInstance(String infrastructureId, String instanceId) {
        restClient.deleteToInstancesWebResource(infrastructureId, "instanceId", instanceId);
    }

    public void terminateInstanceByTag(String infrastructureId, String instanceTag) {
        restClient.deleteToInstancesWebResource(infrastructureId, "instanceTag", instanceTag);
    }

    public void terminateInfrastructure(String infrastructureId) {
        restClient.deleteInfrastructuresWebResource(infrastructureId);
    }

    public String runScriptOnInstance(String infrastructureId, String instanceId, String instanceScriptJson) {
        int count = 0;
        while (true) {
            try {
                return restClient.postToScriptsWebResource(infrastructureId, "instanceId", instanceId,
                        instanceScriptJson);
            } catch (Exception e) {
                if (++count == MAX_RETRIES_IN_CASE_OF_ERROR) {
                    logger.error(e);
                    throw e;
                } else {
                    sleepFor(SLEEP_TIME_RETRIES_IN_CASE_OF_ERROR);
                }
            }
        }

    }

    private void sleepFor(long millisecondsToSleep) {
        try {
            Thread.sleep(millisecondsToSleep);
        } catch (InterruptedException e) {
        }
    }

}
