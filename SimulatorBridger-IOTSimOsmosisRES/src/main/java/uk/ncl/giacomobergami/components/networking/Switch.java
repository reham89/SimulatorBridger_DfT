package uk.ncl.giacomobergami.components.networking;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.osmosis.core.Topology;
import uk.ncl.giacomobergami.utils.data.CSVMediator;

import java.util.List;
import java.util.Map;

@JsonPropertyOrder({"name", "type", "iops"})
public class Switch {
    public String type;  // enum
    public String name;
    public Long iops;

    public LegacyConfiguration.SwitchEntity asLegacySwitchEntity(String global_controller) {
        var result = new LegacyConfiguration.SwitchEntity();
        result.setType(type);
        result.setName(name);
        result.setController(global_controller);
        result.setIops(iops);
        return result;
    }

    public Switch(String type, String name, Long iops) {
        this.type = type;
        this.name = name;
        this.iops = iops;
    }

    public Switch() {}

    public Switch(LegacyConfiguration.SwitchEntity x) {
        type = x.getType();
        name = x.getName();
        iops = x.getIops();
    }


    private static CSVMediator<Switch> readerWriter = null;
    public static CSVMediator<Switch> csvReader() {
        if (readerWriter == null)
            readerWriter = new CSVMediator<>(Switch.class);
        return readerWriter;
    }

    public void initializeSwitch(Map<String, Integer> nameIdTable, Topology topology, List<org.cloudbus.cloudsim.sdn.Switch> switches) {
        long iops = getIops();
        String switchName = getName();
        String switchType = getType();
        org.cloudbus.cloudsim.sdn.Switch sw = new org.cloudbus.cloudsim.sdn.Switch(switchName, switchType, iops);
        nameIdTable.put(switchName, sw.getAddress());
        topology.addNode(sw);
        switches.add(sw);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getIops() {
        return iops;
    }

    public void setIops(Long iops) {
        this.iops = iops;
    }
}
