package org.pillarone.riskanalytics.graph.formeditor.util;

import com.ulcjava.ext.graph.model.Port;
import com.ulcjava.ext.graph.model.Vertex;
import com.ulcjava.ext.graph.shared.PortAlignment;
import com.ulcjava.ext.graph.shared.PortConstraint;
import com.ulcjava.ext.graph.shared.PortType;
import com.ulcjava.base.application.util.Dimension;
import com.ulcjava.base.application.util.Point;
import com.ulcjava.base.application.util.Rectangle;
import org.pillarone.riskanalytics.graph.core.graph.model.ComponentNode;
import org.pillarone.riskanalytics.graph.core.graph.model.ComposedComponentNode;
import org.pillarone.riskanalytics.graph.core.graph.model.InPort;
import org.pillarone.riskanalytics.graph.core.graph.model.OutPort;
import org.pillarone.riskanalytics.graph.core.graph.util.IntegerRange;
import org.pillarone.riskanalytics.graph.core.graph.util.UIUtils;
import org.pillarone.riskanalytics.graph.core.graph.wiringvalidation.WiringValidationUtil;
import org.pillarone.riskanalytics.graph.core.palette.model.ComponentDefinition;
import org.pillarone.riskanalytics.graph.core.palette.service.PaletteService;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * 
 */
public class VisualSceneUtilities {

    public static Vertex createVertex(Point mouseLocation, ComponentNode graphNode) {
        String componentTypePath = graphNode.getType().getTypeClass().getName();
        Vertex vertex = new Vertex("internal" + new Date().getTime() + "_" + Math.random());
        vertex.setStyle("swimlane");
        if (mouseLocation==null) {
            mouseLocation = new Point(0,0);
        }
        vertex.setRectangle(new Rectangle(mouseLocation, new Dimension(200, 200)));
        vertex.setTemplateId(componentTypePath);
        vertex.setShowExpandIcon(graphNode instanceof ComposedComponentNode);

        // add the ports
        ComponentDefinition definition = PaletteService.getInstance().getComponentDefinition(componentTypePath);
        for (Map.Entry<Field, Class> entry : GroovyUtils.obtainPorts(definition, "in").entrySet()) {
            String id = "in_port_" + new Date().getTime() + "_"+Math.random();

            Port port = new Port(id, PortType.IN, PortAlignment.LEFT, entry.getValue().getName(), UIUtils.formatDisplayName(entry.getKey().getName()));
            IntegerRange range = WiringValidationUtil.getConnectionCardinality(entry.getKey());
            port.addConstraint(new PortConstraint(entry.getValue().getName(), range != null ? range.getFrom() : 0, range != null ? range.getTo() : Integer.MAX_VALUE));
            vertex.addPort(port);
        }
        for (Map.Entry<Field, Class> entry : GroovyUtils.obtainPorts(definition, "out").entrySet()) {
            String id = "out_port_" + new Date().getTime() + "_"+Math.random();
            Port port = new Port(id, PortType.OUT, PortAlignment.RIGHT, entry.getValue().getName(), UIUtils.formatDisplayName(entry.getKey().getName()));
            port.addConstraint(new PortConstraint(entry.getValue().getName(), 0, Integer.MAX_VALUE));
            vertex.addPort(port);
        }
        return vertex;
    }

    public static boolean isConsistentPort(Port ulcPort, org.pillarone.riskanalytics.graph.core.graph.model.Port graphModelPort) {
        boolean isConsistent = ulcPort.getTitle().equals(UIUtils.formatDisplayName(graphModelPort.getName()));
        if (isConsistent) {
            isConsistent = ((ulcPort.getType()==PortType.REPLICATE_IN || ulcPort.getType()==PortType.IN)
                                    && graphModelPort instanceof InPort)
                                || (ulcPort.getType()==PortType.REPLICATE_OUT || ulcPort.getType()==PortType.OUT)
                                    && graphModelPort instanceof OutPort;
        }
        return isConsistent;
    }

}
