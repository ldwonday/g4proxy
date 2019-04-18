package com.virjar.g4proxy.server;

import com.google.common.base.Splitter;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理空余的port资源
 */
public class AvailablePortResManager {

    private static Set<Integer> availablePortResource = Sets.newConcurrentHashSet();
    private static BlockingDeque<Integer> portQueue = new LinkedBlockingDeque<>();

    private static AtomicBoolean inited = new AtomicBoolean(false);

    private static final String defaultSpaceConfig = "20000-25000";

    private static Map<String, Integer> allocatedResources = Maps.newConcurrentMap();


    public static void init(String spaceConfig) {
        if (inited.compareAndSet(false, true)) {
            initInternal(spaceConfig);
        }
    }

    private static void initInternal(String spaceConfig) {
        Iterable<String> pairs = Splitter.on(":").split(spaceConfig);
        for (String pair : pairs) {
            if (pair.contains("-")) {
                int index = pair.indexOf("-");
                String startStr = pair.substring(0, index);
                String endStr = pair.substring(index + 1);
                int start = Integer.parseInt(startStr);
                int end = Integer.parseInt(endStr);
                for (int i = start; i <= end; i++) {
                    availablePortResource.add(i);
                }
            } else {
                availablePortResource.add(Integer.parseInt(pair));
            }
        }


        portQueue.addAll(availablePortResource);
    }

    public static void clearBindRelationShip(String clientId) {
        allocatedResources.remove(clientId);
    }

    public synchronized static Integer allocate(String clientId) {
        if (allocatedResources.containsKey(clientId)) {
            Integer integer = allocatedResources.get(clientId);
            if (integer != null) {
                return integer;
            }
        }

        init(defaultSpaceConfig);
        Integer ret = portQueue.poll();
        allocatedResources.put(clientId, ret);
        return ret;
    }

}
