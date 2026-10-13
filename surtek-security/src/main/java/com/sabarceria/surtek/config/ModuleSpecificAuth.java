package com.sabarceria.surtek.config;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Data
@Builder(builderClassName = "ClassBuilder")
public class ModuleSpecificAuth {

    private ConcurrentHashMap<String, CopyOnWriteArrayList<String>> modules;

    public static ClassBuilder builder(){
        return new ClassBuilder()
                .modules(new ConcurrentHashMap<>());
    }

    public static class ClassBuilder{
        public ClassBuilder addEndpoint(String username, String endpoints){
            CopyOnWriteArrayList<String> endpointList = this.modules.get(endpoints);
            endpointList.add(endpoints);
            this.modules.put(username, endpointList);
            return this;
        }

        public ClassBuilder addEndpoint(String username,CopyOnWriteArrayList<String> endpoints){
            this.modules.put(username, endpoints);
            return this;
        }

        public ClassBuilder addModule(String username, CopyOnWriteArrayList<String> endpoints){
            this.modules.put(username, endpoints);
            return this;
        }
    }

    public void updateEndpoints(String keyName, CopyOnWriteArrayList<String> endpoints){
        this.modules.put(keyName, endpoints);
    }

    public List<String> getEndpoints(String keyName){
        return this.modules.getOrDefault(keyName, new CopyOnWriteArrayList<>());
    }
}
