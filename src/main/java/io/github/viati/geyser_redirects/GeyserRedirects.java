package io.github.viati.geyser_redirects;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.geysermc.floodgate.api.FloodgateApi;
import org.slf4j.Logger;

import java.io.File;
import java.nio.file.Path;

@Plugin(
        id = "geyser_redirects",
        name = "Geyser Redirects",
        version = "1.0-SNAPSHOT"
)
public class GeyserRedirects {

    public static String javaServerName = "au2je";
    public static String bedrockServerName = "au2be";
    public static CommentedFileConfig config;
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final FloodgateApi floodgate = FloodgateApi.getInstance();

    @Inject
    public GeyserRedirects(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitializeEvent(ProxyInitializeEvent event) {
        config = CommentedFileConfig.builder(new File(dataDirectory.toFile(), "geyser_redirects.toml")).defaultResource("defaultConfig.toml").autosave().autoreload().build();
        config.load();

        if (config.contains("java")) {
            javaServerName = config.get("java");
        } else {
            config.set("java", javaServerName);
            logger.warn("Missing java server key, falling back to {}", javaServerName);
        }

        if (config.contains("bedrock")) {
            bedrockServerName = config.get("bedrock");
        } else {
            config.set("bedrock", bedrockServerName);
            logger.warn("Missing bedrock server key, falling back to {}", bedrockServerName);
        }
    }

    @Subscribe
    public void onProxyShutdownEvent(ProxyShutdownEvent event) {
        config.close();
    }

    @Subscribe
    public void onServerPreConnectEvent(ServerPreConnectEvent event) {
        String preServerName = event.getOriginalServer().getServerInfo().getName();
        if (preServerName.equals(javaServerName)
                || preServerName.equals(bedrockServerName)) {
            String serverName = floodgate.isFloodgatePlayer(event.getPlayer().getUniqueId()) ? bedrockServerName : javaServerName;
            if (serverName.equals(preServerName)) return;
            if (server.getServer(serverName).isEmpty()) {
                event.getPlayer().disconnect(Component.text("Couldn't find the server.", TextColor.color(232, 19, 19)));
                return;
            }
            logger.info("Attempting to redirect {} <{}> to {}", event.getPlayer().getGameProfile().getName(), event.getPlayer().getUniqueId().toString(), serverName);
            event.setResult(ServerPreConnectEvent.ServerResult.denied());
            event.getPlayer().createConnectionRequest(server.getServer(serverName).get()).fireAndForget();
        }
    }
}
