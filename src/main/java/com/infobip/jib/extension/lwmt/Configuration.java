package com.infobip.jib.extension.lwmt;

import java.util.ArrayList;
import java.util.List;

public class Configuration {

    private final List<String> filters;

    public Configuration() {
        this.filters = new ArrayList<>();
    }

    public Configuration(List<String> filters) {
        this.filters = filters;
    }

    public List<String> getFilters() {
        return filters;
    }

    public boolean isEmpty() {
        return filters.isEmpty();
    }
}
