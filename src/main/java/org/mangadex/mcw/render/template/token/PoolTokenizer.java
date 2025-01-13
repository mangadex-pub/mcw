package org.mangadex.mcw.render.template.token;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import org.mangadex.mcw.render.RenderProperties;

@Component
public class PoolTokenizer {

    private static final Logger LOGGER = LoggerFactory.getLogger(PoolTokenizer.class);

    private static final String S_DNSA_NPORT_PREFIX = "dns://";
    private static final String S_DNSA_YPORT_PREFIX = "dns+";
    private static final Pattern S_DNSA_YPORT_PATTERN = Pattern.compile("dns\\+(?<port>.+)?://(?<host>.+)");
    private static final String S_DNSSRV_PREFIX = "dnssrv://";

    private final RenderProperties settings;

    public PoolTokenizer(RenderProperties settings) {
        this.settings = settings;
    }

    public List<ServerToken> tokenize(String name, JsonNode pool) {
        LOGGER.trace("Tokenizing pool[{}]", name);

        if (pool == null || pool.isNull()) {
            throw new PoolTokenizationException(name, "is null");
        }

        if (!pool.isObject()) {
            throw new PoolTokenizationException(name, "is not a json object (was: " + pool.getNodeType() + ")");
        }

        if (!pool.has("servers")) {
            throw new PoolTokenizationException(name, "has no 'servers' field");
        }

        var servers = pool.get("servers");
        if (servers.isNull()) {
            throw new PoolTokenizationException(name, "$.servers is null");
        }

        if (!servers.isArray()) {
            throw new PoolTokenizationException(name, "$.servers is not a json array (was: " + servers.getNodeType() + ")");
        }

        int poolServersCount = servers.size();

        var failedParses = new ArrayList<String>();
        var successfulParses = new ArrayList<ServerToken>();

        for (int i = 0; i < poolServersCount; i++) {
            var serverItem = servers.get(i);
            if (serverItem.isNull()) {
                failedParses.add("- servers[" + i + "] is null");
                continue;
            } else if (!serverItem.isTextual()) {
                failedParses.add("- servers[" + i + "] is not a string (was: " + serverItem.getNodeType() + ")");
                continue;
            }

            String serverItemString = serverItem.textValue();
            if (serverItemString.isBlank()) {
                failedParses.add("- servers[" + i + "] is a blank string");
            }

            var serverToken = parseServer(serverItemString);
            if (serverToken == null) {
                failedParses.add("- servers[" + i + "] is not a valid server token (from: '" + serverItemString + "')");
            }

            LOGGER.trace("+ pools[{}]->servers[{}]='{}' -> {}", name, i, serverItemString, serverToken);
            successfulParses.add(serverToken);
        }

        if (!failedParses.isEmpty()) {
            throw new PoolTokenizationException(name, "Some servers in $.servers were not successfully parsed:\n" + failedParses);
        }

        return successfulParses;
    }

    private ServerToken parseServer(String server) {
        if (server.startsWith(S_DNSSRV_PREFIX)) {
            return new DNSSRVToken(server.substring(S_DNSSRV_PREFIX.length()));
        } else if (server.startsWith(S_DNSA_NPORT_PREFIX)) {
            return new DNSAToken(server.substring(S_DNSA_NPORT_PREFIX.length()), this.settings.defaultPort());
        }

        if (server.startsWith(S_DNSA_YPORT_PREFIX)) {
            var matcher = S_DNSA_YPORT_PATTERN.matcher(server);
            if (matcher.matches()) {
                var host = matcher.group("host");
                var portStr = matcher.group("port");

                var port = -1;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    LOGGER.warn("Invalid server port specification '{}' (from server string: '{}')", portStr, server, e);
                    return null;
                }

                if (port < 0 || port > 65535) {
                    LOGGER.warn("Invalid unix port number {} (from server string: '{}')", port, server);
                    return null;
                }

                return new DNSAToken(host, port);
            }
        }

        return new FixedToken(server);
    }

}
