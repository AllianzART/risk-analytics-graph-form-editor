package org.pillarone.riskanalytics.graph.formeditor.ui.model.filters;

import org.pillarone.riskanalytics.graph.formeditor.ui.model.IComponentNodeFilter;

/**
 * 
 */
public class ComponentNodeFilterFactory {
    public static final String COMPONENT_NAME = "Component Name";
    public static final String CATEGORY = "Category";
    public static final String NONE = "None";
    private static String[] FILTER_NAMES = {NONE, COMPONENT_NAME, CATEGORY};

    public static IComponentNodeFilter getFilter(String modelName, String value) {
        if (modelName.equalsIgnoreCase(COMPONENT_NAME)) {
            return new NamePatternComponentNodeFilter(value);
        } else {
            return new NoneComponentNodeFilter();
        }
    }

    public static String[] getFilterModelNames() {
        return FILTER_NAMES;
    }
}
