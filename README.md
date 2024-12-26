# MCW - MCRouter Config Watcher

#### Dynamic DNS (re)configuration for MCRouter

**STATUS:** !! Experimental !!

## Summary

MCRouter is great, but it resolves server hosts only once, via simple A/AAAA hostname resolution, and never cares again later.

This is a problem in many environments where target memcache instances might come and go, or change IPs from time to time. Especially in a Kubernetes
environment, it's best to be able to rely on things like DNS SRV records exposed by headless services.

MCW is designed to make this problem go away entirely, in a robust and reasonably flexible way.

## How It Works

The mode of operation of MCW is as follows:

1. Watch a configuration template
2. On change and on a schedule, re-resolve dynamic tokens it contains
3. Output a rendered configuration with only IP:Port formatted servers

For example, with the following template:

```json
{
  "pools": {
    "kubernetes": {
      "servers": [
        "dns://memcache.domain"
      ]
    }
  }
}
```

If `memcache.domain` contains the following A records:

- 10.0.0.1
- 10.0.0.2
- 10.0.0.3

Then MCW will output the following configuration:

```json
{
  "pools": {
    "kubernetes": {
      "servers": [
        "10.0.0.1:11211",
        "10.0.0.2:11211",
        "10.0.0.3:11211"
      ]
    }
  }
}
```

### Supported Tokens

The following server tokens are supported:

- `dns://domain.example`: Will result in a server per A record on `domain.example`, with port `11211`
- `dns+1234://domain.example` Same as above, but with port `1234`
- `dnssrv://_memcache._tcp.domain.example` Will use the records returned by an SRV query of `_memcache._tcp.domain.example`

Finally, any server not matching these tokens is kept as-is, and you can safely mix-and-match static and dynamic server entries.

## Quick Start

Write your MCRouter configuration template using [server tokens](#supported-tokens), then run:

```text
/path/to/mcw
  --source file:///configuration/mcrouter/template.json
  --output file:///configuration/mcrouter/configuration.json
```

And MCW will automatically:

1. Read `template.json`
2. Resolve servers it references
3. Render `configuration.json`

Then, anytime `template.json` changes, or every `render-ttl`, it will update `configuration.json` accordingly.

## Server Ordering

MCW attempts to keep a stable server order, as such dynamic tokens are only ever expanded in-place, and are not deduplicated (so if a server shows up as part of
2 different dynamic tokens in the same pool, the pool will have 2 entries pointing to it).

### For A-based Server Records (`dns[+port]://`)

MCW takes the record in the order they are returned to it.

Since `dns://` and `dns+port://` servers are resolved via an `A` DNS query, and do not provide any metadata (such a name) for the underlying servers, you must
ensure that your resolver returns a consistently ordered list of records.

> This is highly uncommon, and the majority of DNS resolvers and nameservers return A records in a randomized order.

### For SRV-based Server Records (`dnssrv://`)

SRV records are sorted by:

1. Their priority, then
2. Their target, lexicographically

For example, given the following SRV response:

```text
_memcache._tcp.memcache-srv IN SRV 10 100 11211 memcache-B
_memcache._tcp.memcache-srv IN SRV 50 100 11211 memcache-C
_memcache._tcp.memcache-srv IN SRV 10 100 11211 memcache-A
```

MCW will render `[memcache-A, memcache-B, memcache-C]`

> Note 1: The weight value of SRV records is ignored
>
> Note 2: If an SRV record has a target that resolves to multiple A records, they will all be included, and their sorting follows A-based sorting logic. But
> also don't do that to yourself, life's too short...

For best efficiency, ensure you always return the same _set_ of SRV records. For example, it is best to not remove an SRV record for a server going away 10
seconds during a rolling restart, and to instead rely on MCRouter's native healthchecking of servers.

In a Kubernetes environment, you can ensure an SRV record for any pod in a StatefulSet, no matter the pod's readiness, with the following headless Service
parameter:

```yaml
apiVersion: v1
kind: Service
# ...
spec:
  publishNotReadyAddresses: true
  # ...
```
