package org.mangadex.mcw.dns;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.Type;
import org.xbill.DNS.lookup.LookupResult;
import org.xbill.DNS.lookup.LookupSession;

import org.mangadex.mcw.dns.request.DnsRequest;
import org.mangadex.mcw.dns.request.RequestType;
import org.mangadex.mcw.dns.response.DnsResolution;
import org.mangadex.mcw.dns.response.DnsResolutionFailure;
import org.mangadex.mcw.dns.response.DnsResolutionSuccess;

@Component
public class DnsResolver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DnsResolver.class);
    private static final ExecutorService DNS_THREADS = Executors.newWorkStealingPool(1);
    private static final Comparator<Record> SORT_BY_NAME = Comparator.comparing(Record::getName);

    private final LookupSession lookupSession;

    public DnsResolver(Resolver resolver) {
        this.lookupSession = LookupSession.defaultBuilder().resolver(resolver).build();
    }

    public DnsResolution resolve(DnsRequest request) {
        var result = DNS_THREADS.submit(() -> resolveInternal(request));
        try {
            return result.get();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e.getCause());
        }
    }

    private DnsResolution resolveInternal(DnsRequest request) {
        var lookup = lookupSession.lookupAsync(request.qname(), request.type().code);

        var resolution = switch (request.type()) {
            case RequestType.A -> lookup.thenApply(this::handleA);
            case RequestType.SRV -> lookup.thenApply(this::handleSRV);
        };

        return resolution.exceptionally(
            // unwrap completion exceptions
            e -> e instanceof CompletionException ce
                ? new DnsResolutionFailure(ce.getCause())
                : new DnsResolutionFailure(e)
        ).toCompletableFuture().join();
    }

    private DnsResolution handleA(LookupResult result) {
        List<Record> aRecords = result
            .getRecords()
            .stream()
            .peek(r -> {
                if (r.getType() != Type.A) {
                    LOGGER.warn("Unexpected non-A record discarded: {}", r);
                } else {
                    LOGGER.debug("+ A record {}", r);
                }
            })
            .filter(r -> r.getType() == Type.A)
            .sorted(SORT_BY_NAME)
            .toList();

        return new DnsResolutionSuccess(aRecords);
    }

    private DnsResolution handleSRV(LookupResult result) {
        List<Record> srvRecords = result
            .getRecords()
            .stream()
            .peek(r -> {
                if (r.getType() != Type.SRV) {
                    LOGGER.debug("Unexpected non-SRV response in SRV query: {}", r);
                } else {
                    LOGGER.debug("+ SRV record {}", r);
                }
            })
            .filter(r -> r.getType() == Type.SRV)
            .sorted(SORT_BY_NAME)
            .toList();

        return new DnsResolutionSuccess(srvRecords);
    }

}
