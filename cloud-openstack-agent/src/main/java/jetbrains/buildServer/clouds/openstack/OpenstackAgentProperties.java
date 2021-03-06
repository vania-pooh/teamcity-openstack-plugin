package jetbrains.buildServer.clouds.openstack;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.AgentLifeCycleAdapter;
import jetbrains.buildServer.agent.BuildAgent;
import jetbrains.buildServer.agent.BuildAgentConfiguration;
import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.EventDispatcher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class OpenstackAgentProperties extends AgentLifeCycleAdapter {
    @NotNull private static final Logger LOG = Loggers.AGENT;
    @NotNull private final String metadataUrl = "http://169.254.169.254/openstack/latest/meta_data.json";

    public OpenstackAgentProperties(@NotNull EventDispatcher<AgentLifeCycleAdapter> dispatcher) {
        dispatcher.addListener(this);
    }

    private static JsonElement readJsonFromUrl(String sURL) throws IOException {
        URL url = new URL(sURL);
        HttpURLConnection request = (HttpURLConnection) url.openConnection();
        request.connect();
        JsonParser jp = new JsonParser();
        return jp.parse(new InputStreamReader((InputStream) request.getContent()));
    }

    @Override
    public void beforeAgentConfigurationLoaded(@NotNull BuildAgent agent) {
        BuildAgentConfiguration configuration = agent.getConfiguration();

        try {
            JsonElement json = readJsonFromUrl(metadataUrl);
            String uuid = json.getAsJsonObject().get("uuid").toString();
            if (uuid != null && !uuid.trim().isEmpty()) {
                LOG.debug(String.format("Detected Openstack instance. Will write parameters from metadata: %s", metadataUrl));
                uuid = uuid.replaceAll("^\"|\"$", "");  // trim leading and ending double quotes
                configuration.addConfigurationParameter("agent.cloud.type", OpenstackCloudParameters.CLOUD_TYPE);
                configuration.addConfigurationParameter(OpenstackCloudParameters.OPENSTACK_INSTANCE_ID, uuid);
            }
        } catch (IOException e) {
            LOG.debug("Agent launched at non-Openstack instance.");
            LOG.error(e.getMessage());
        }

    }

}
