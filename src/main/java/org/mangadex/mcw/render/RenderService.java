package org.mangadex.mcw.render;

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.Comparator.comparing;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Record;
import org.xbill.DNS.SRVRecord;
import org.xbill.DNS.Type;

import org.mangadex.mcw.dns.DnsResolver;
import org.mangadex.mcw.dns.request.DnsRequest;
import org.mangadex.mcw.dns.request.RequestType;
import org.mangadex.mcw.dns.response.DnsResolutionFailure;
import org.mangadex.mcw.dns.response.DnsResolutionSuccess;
import org.mangadex.mcw.render.template.InvalidTemplateException;
import org.mangadex.mcw.render.template.ResolvedServer;
import org.mangadex.mcw.render.template.token.DNSAToken;
import org.mangadex.mcw.render.template.token.DNSSRVToken;
import org.mangadex.mcw.render.template.token.FixedToken;
import org.mangadex.mcw.render.template.token.PoolTokenizer;
import org.mangadex.mcw.render.template.token.ServerToken;

@Component
public class RenderService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RenderService.class);

    private static final Comparator<SRVRecord> SRV_ORDERING = comparing(SRVRecord::getPriority).thenComparing(SRVRecord::getTarget);

    private final DnsResolver resolver;
    private final ObjectMapper objectMapper;
    private final PoolTokenizer tokenizer;
    private final RenderProperties renderProperties;

    public RenderService(DnsResolver resolver, ObjectMapper objectMapper, PoolTokenizer tokenizer, RenderProperties renderProperties) {
        this.resolver = resolver;
        this.objectMapper = objectMapper;
        this.tokenizer = tokenizer;
        this.renderProperties = renderProperties;
    }

    public Render render(String template) throws IOException {
        LOGGER.trace("Rendering template\n{}", template);

        var root = objectMapper.readTree(template);
        long minDnsTTL = Long.MAX_VALUE;

        var pools = root.get("pools");
        if (pools == null || !pools.isObject() || pools.isEmpty()) {
            throw new InvalidTemplateException("Cannot parse configuration template, $.pools must be non-empty object");
        }

        var poolEntries = pools.fields();
        while (poolEntries.hasNext()) {
            var poolEntry = poolEntries.next();
            var poolName = poolEntry.getKey();
            var pool = poolEntry.getValue();
            LOGGER.trace("Processing pool[{}]", poolName);

            var serverTokens = tokenizer.tokenize(poolName, pool);
            LOGGER.trace("Tokenized pool[{}]->servers into {}", poolName, serverTokens);

            var poolServers = serverTokens.stream().map(this::resolve).flatMap(List::stream).toList();
            var poolTTL = poolServers
                .stream()
                .mapToLong(ResolvedServer::ttl)
                .min()
                .orElse(Long.MAX_VALUE); // empty result, essentially
            LOGGER.debug("Resolved pool[{}]->servers into {} (ttl={}s)", poolName, poolServers, poolTTL);

            minDnsTTL = min(poolTTL, minDnsTTL);

            var poolHostPorts = poolServers.stream().map(ResolvedServer::value).map(TextNode::new).toList();
            LOGGER.debug("Final pool[{}]->servers = {}", poolName, poolHostPorts);

            var serversNode = pool.withArrayProperty("servers");
            serversNode.removeAll();
            serversNode.addAll(poolHostPorts);
        }

        long minTTL = renderProperties.ttl().min();
        long maxTTL = renderProperties.ttl().max();
        var renderTTL = max(minTTL, min(minDnsTTL, maxTTL));

        var rendered = objectMapper.writeValueAsString(root);
        LOGGER.debug("Rendered template (ttl={}s) with content \n{}", renderTTL, rendered);
        return new Render(rendered, renderTTL);
    }

    private List<ResolvedServer> resolve(ServerToken token) {
        LOGGER.trace("Resolving token {}", token);
        return switch (token) {
            case FixedToken ft -> List.of(new ResolvedServer(ft.value(), ft, Long.MAX_VALUE));
            case DNSAToken a -> resolveDNSA(a);
            case DNSSRVToken a -> resolveDNSSRV(a);
        };
    }

    private List<ResolvedServer> resolveDNSSRV(DNSSRVToken token) {
        var srvRecords = dnsquery(RequestType.SRV, token.value())
            .stream()
            // do not consume extra answers, such as additional names,
            // since they are not guaranteed to be exhaustive
            .filter(r -> r.getType() == Type.SRV)
            .map(SRVRecord.class::cast)
            .sorted(SRV_ORDERING)
            .toList();

        if (srvRecords.isEmpty()) {
            return Collections.emptyList();
        }

        var srvTTL = srvRecords
            .stream()
            .mapToLong(SRVRecord::getTTL)
            .min()
            .orElseThrow();

        return srvRecords
            .stream()
            .map(srv -> new DNSAToken(srv.getTarget().toString(true), srv.getPort()))
            .map(srvA -> resolveAName(srvA.value(), srvA.port(), token, srvTTL))
            .flatMap(List::stream)
            .toList();
    }

    private List<ResolvedServer> resolveDNSA(DNSAToken token) {
        return resolveAName(token.value(), token.port(), token, Long.MAX_VALUE);
    }

    private List<ResolvedServer> resolveAName(String qname, int port, ServerToken source, long maxTTL) {
        var records = dnsquery(RequestType.A, qname);

        if (records.isEmpty()) {
            return Collections.emptyList();
        }

        /*
         * The TTL of a server is the minimum between:
         * 1. Its response record TTL
         * 2. The response TTL of its ancestor record (in the case of SRV->A type steps)
         */
        long recordsTTL = records.stream().mapToLong(Record::getTTL).min().orElseThrow();
        long ttl = min(recordsTTL, maxTTL);

        return records
            .stream()
            .map(Record::rdataToString)
            .map(ip -> ip + ":" + port)
            .map(hostport -> new ResolvedServer(hostport, source, ttl))
            .toList();
    }

    private List<Record> dnsquery(RequestType qtype, String qname) {
        var resolution = resolver.resolve(new DnsRequest(qtype, qname));
        return switch (resolution) {
            case DnsResolutionFailure(Throwable cause) -> throw new RuntimeException("Cannot resolve " + qtype + " " + qname, cause);
            case DnsResolutionSuccess(var records) -> records;
        };
    }

}
