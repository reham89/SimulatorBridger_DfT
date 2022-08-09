package uk.ncl.giacomobergami.components.simulator;

import org.cloudbus.agent.AgentBroker;
import org.cloudbus.agent.config.AgentConfigLoader;
import org.cloudbus.agent.config.AgentConfigProvider;
import org.cloudbus.agent.config.TopologyLink;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.MainEventManager;
import org.cloudbus.cloudsim.edge.core.edge.LegacyConfiguration;
import org.cloudbus.cloudsim.edge.utils.LogUtil;
import org.cloudbus.cloudsim.osmesis.examples.uti.LogPrinter;
import org.cloudbus.cloudsim.osmesis.examples.uti.PrintResults;
import org.cloudbus.cloudsim.osmesis.examples.uti.RESPrinter;
import org.cloudbus.cloudsim.sdn.Switch;
import org.cloudbus.osmosis.core.*;
import org.cloudbus.res.EnergyController;
import org.cloudbus.res.config.AppConfig;
import org.cloudbus.res.dataproviders.res.RESResponse;
import uk.ncl.giacomobergami.components.loader.GlobalConfigurationSettings;
import uk.ncl.giacomobergami.components.mel_routing.MELRoutingPolicy;
import uk.ncl.giacomobergami.components.mel_routing.MELRoutingPolicyGeneratorFacade;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class provides an abstraction over the possible settings of running OSMOSIS
 */
public class OsmoticWrapper {
    private OsmoticConfiguration conf;
    LegacyTopologyBuilder topologyBuilder;
    OsmoticBroker osmesisBroker;
    AgentBroker agentBroker;
    Map<String, EnergyController> energyControllers;
    private boolean init;
    private boolean started;
    private boolean finished;
    private double runTime;
    List<OsmoticAppDescription> appList;



    public OsmoticWrapper() {
        this(null);
    }

    public OsmoticWrapper(OsmoticConfiguration conf) {
        this.conf = conf;
        init = false;
        started = false;
        finished = false;
        energyControllers = null;
        runTime = 0.0;
    }

    private static File fileExists(String path) {
        if (path != null) {
            File n = new File(path).getAbsoluteFile();
            if (n.exists() && n.isFile()) {
                return n;
            }
        }
        return null;
    }

    public void stop() {
        if (started) {
            MainEventManager.novel_stop();
//            OsmoticAppsParser.appList.clear();
            OsmoticBroker.workflowTag.clear();
            osmesisBroker = null;
            topologyBuilder = null;
            agentBroker = null;
            started = false;
            init = false;
            energyControllers = null;
            runTime = 0.0;
            started = false;
            finished = false;
        }
    }

    public boolean runConfiguration(OsmoticConfiguration newConfiguration) {
        stop();
        init = false;
        this.conf = newConfiguration;
        if (!init()) {
            stop();
            return false;
        }
        start();
        return true;
    }

    private boolean init() {
        stop(); // ensuring that the previous simulation was stopped
        if (init) return init;
        Calendar calendar = Calendar.getInstance();

        // Getting configuration from json and entering classes to Agent Broker
        if (agentBrokerageInitFails()) return init;

        allocateOrClearDataStructures(calendar);

        osmesisBroker = LegacyTopologyBuilder.newBroker(); // TODO: new OsmoticBroker(conf.OsmesisBroker, edgeLetId);
        MELRoutingPolicy melSwitchPolicy = MELRoutingPolicyGeneratorFacade.generateFacade(conf.mel_switch_policy);
        osmesisBroker.setMelRouting(melSwitchPolicy);

        topologyBuilder = new LegacyTopologyBuilder(osmesisBroker);
        {
            var confFile = fileExists(conf.configurationFile);
            if (confFile != null) {
                LegacyConfiguration config = LegacyConfiguration.fromFile(confFile);
                if(config !=  null) {
                    try {
                        topologyBuilder.buildTopology(config);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return init;
                    }
                }
            }
        }


        OsmosisOrchestrator conductor = new OsmosisOrchestrator();
        appList = OsmoticAppsParser.legacyAppParser(conf.osmesisAppFile);

        List<SDNController> controllers = new ArrayList<>();
        for(OsmoticDatacenter osmesisDC : topologyBuilder.getOsmesisDatacentres()){
            osmesisBroker.submitVmList(osmesisDC.getVmList(), osmesisDC.getId());
            controllers.add(osmesisDC.getSdnController());
            osmesisDC.getSdnController().setWanOorchestrator(conductor);
        }
        controllers.add(topologyBuilder.getSdWanController());
        conductor.setSdnControllers(controllers);
        osmesisBroker.submitOsmesisApps(appList);
        osmesisBroker.setDatacenters(topologyBuilder.getOsmesisDatacentres());

        init = true;
        return init;
    }

    private void allocateOrClearDataStructures(Calendar calendar) {
        MainEventManager.init(conf.num_user, calendar, conf.trace_flag);
        if (conf.terminate_simulation_at > 0)
            MainEventManager.terminateSimulation(conf.terminate_simulation_at);
    }

    /**
     * This part only initializes the AgentBrokering architecture, and not the actual simulator.
     * @return
     */
    private boolean agentBrokerageInitFails() {
        if (fileExists(conf.AGENT_CONFIG_FILE) != null) {
            // Set Agent and Message classes
            AgentBroker agentBroker = AgentBroker.getInstance();

            // Getting configuration from json and entering classes to Agent Broker
            AgentConfigProvider provider = null;
            try {
                provider = new AgentConfigProvider(AgentConfigLoader.getFromFile(conf.AGENT_CONFIG_FILE));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return true;
            }

            // In this example, the Central Agent is not used
            try {
                agentBroker.setDcAgentClass(provider.getDCAgentClass());
                agentBroker.setDeviceAgentClass(provider.getDeviceAgentClass());
                agentBroker.setAgentMessageClass(provider.getAgentMessageClass());
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return true;
            }

            //Simulation is not started yet thus there is not any MELs.
            //Links for Agents between infrastructure elements.
            for (TopologyLink link : provider.getTopologyLinks()) {
                agentBroker.addAgentLink(link.AgentA, link.AgentB);
            }

            //Osmotic Agents time interval
            agentBroker.setMAPEInterval(provider.getMAPEInterval());

            if (fileExists(conf.RES_CONFIG_FILE) != null) {
                RESResponse resResponse = null;
                try {
                    resResponse = AppConfig.RES_PARSER.parse(conf.RES_CONFIG_FILE);
                } catch (IOException e) {
                    e.printStackTrace();
                    return true;
                }
                energyControllers = resResponse
                        .getDatacenters()
                        .stream()
                        .map(EnergyController::fromDatacenter)
                        .collect(Collectors.toMap(EnergyController::getEdgeDatacenterId, Function.identity()));
                agentBroker.setEnergyControllers(energyControllers);
            }

            if (conf.simulationStartTime != null && (!conf.simulationStartTime.isEmpty())) {
                agentBroker.setSimulationStartTime(conf.simulationStartTime);
            }
        }
        return false;
    }

    private void start() {
        init(); // Ensuring that the simulation is started
        runTime = MainEventManager.startSimulation();
        finished = true;
    }

    public void legacy_log() {
        if (finished) {
            LogUtil.simulationFinished();
            PrintResults pr = new PrintResults();
            pr.printOsmesisNetwork(appList);

            Log.printLine();

            for(OsmoticDatacenter osmesisDC : topologyBuilder.getOsmesisDatacentres()){
                List<Switch> switchList = osmesisDC.getSdnController().getSwitchList();
                LogPrinter.printEnergyConsumption(osmesisDC.getName(), osmesisDC.getSdnhosts(), switchList, runTime);
                Log.printLine();
            }

            Log.printLine();
            LogPrinter.printEnergyConsumption(topologyBuilder.getSdWanController().getName(), null, topologyBuilder.getSdWanController().getSwitchList(), runTime);
            Log.printLine();
            Log.printLine("Simulation Finished!");

            Log.printLine();
            Log.printLine("Post-mortem RES energy analysis!");

            if (energyControllers != null) {
                RESPrinter res_printer = new RESPrinter();
                res_printer.postMortemAnalysis(energyControllers, conf.simulationStartTime, true,1, appList);
            }
            //res_printer.postMortemAnalysis(energyControllers,simulationStartTime, false, 36);
            //res_printer.postMortemAnalysis(energyControllers,"20160901:0000", false, 36);
            Log.printLine("End of RES analysis!");
        }
    }


    public void log(GlobalConfigurationSettings conf) {
        if (finished) {
            LogUtil.simulationFinished();
            PrintResults pr = new PrintResults();
            pr.printOsmesisNetwork(appList);

            Log.printLine();

            for(OsmoticDatacenter osmesisDC : conf.conf.osmesisDatacentres){
                List<Switch> switchList = osmesisDC.getSdnController().getSwitchList();
                LogPrinter.printEnergyConsumption(osmesisDC.getName(), osmesisDC.getSdnhosts(), switchList, runTime);
                Log.printLine();
            }

            Log.printLine();
            LogPrinter.printEnergyConsumption(conf.sdWanController.getName(), null, conf.sdWanController.getSwitchList(), runTime);
            Log.printLine();
            Log.printLine("Simulation Finished!");

            Log.printLine();
            Log.printLine("Post-mortem RES energy analysis!");

            if (energyControllers != null) {
                RESPrinter res_printer = new RESPrinter();
                res_printer.postMortemAnalysis(energyControllers, conf.simulationStartTime, true,1, appList);
            }
            //res_printer.postMortemAnalysis(energyControllers,simulationStartTime, false, 36);
            //res_printer.postMortemAnalysis(energyControllers,"20160901:0000", false, 36);
            Log.printLine("End of RES analysis!");
        }
    }

    public boolean runConfiguration(GlobalConfigurationSettings conf) {
        stop();
        init = false;
        this.conf = conf.asPreviousOsmoticConfiguration();
        if (!init(conf)) {
            stop();
            return false;
        }
        start();
        return true;
    }

    private boolean init(GlobalConfigurationSettings conf) {
        stop(); // ensuring that the previous simulation was stopped
        if (init) return init;
        Calendar calendar = Calendar.getInstance();

        // Getting configuration from json and entering classes to Agent Broker
        if (agentBrokerageInitFails()) return init;

        allocateOrClearDataStructures(calendar);


        osmesisBroker = conf.newBroker(); // TODO: new OsmoticBroker(conf.OsmesisBroker, edgeLetId);
        MELRoutingPolicy melSwitchPolicy = MELRoutingPolicyGeneratorFacade.generateFacade(conf.mel_switch_policy);
        osmesisBroker.setMelRouting(melSwitchPolicy);
        conf.buildTopologyForSimulator(osmesisBroker);

        OsmosisOrchestrator conductor = new OsmosisOrchestrator();
        List<SDNController> controllers = new ArrayList<>();
        for(OsmoticDatacenter osmesisDC : conf.conf.osmesisDatacentres){
            osmesisBroker.submitVmList(osmesisDC.getVmList(), osmesisDC.getId());
            controllers.add(osmesisDC.getSdnController());
            osmesisDC.getSdnController().setWanOorchestrator(conductor);
        }
        controllers.add(conf.sdWanController);
        conductor.setSdnControllers(controllers);
        appList = osmesisBroker.submitWorkloadCSVApps(conf.apps);
        osmesisBroker.setDatacenters(conf.conf.osmesisDatacentres);

        init = true;
        return init;
    }
}