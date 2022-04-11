package com.github.kbuntrock.model;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Tag {

    private String name;

    private List<Endpoint> endpoints = new ArrayList<>();

    public Tag(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void addEndpoint(final Endpoint endpoint) {
        this.endpoints.add(endpoint);
    }

    @Override
    public String toString() {
        return "Tag{" +
                "name='" + name + '\'' +
                ", endpoints=" + endpoints.stream().map(Endpoint::toString).collect(Collectors.joining(", ")) +
                '}';
    }
}