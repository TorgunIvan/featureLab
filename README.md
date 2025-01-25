
# Build
```
mvn clean install -Dmaven.test.skip=true 
```

# Description

## How to Share MDC from ThreadLocal to a Reactive Thread When Using WebClient in a Spring Web MVC-Based Application

This repository demonstrates how to log MDC in Spring Web MVC applications using Logbook,
and how to share MDC from a ThreadLocal to a Reactive Thread when using WebClient (which is based on Spring WebFlux and is reactive under the hood).
I’m not entirely sure the MDC context is fully cleared at the right moment without disrupting the current reactive threads,
since they might reuse the same MDC context.

As you can see, the logs for outgoing (client-side) requests sent by WebClient and recorded by Logbook do not contain MDC keys, 
because we're switching from ThreadLocal to the Reactive Thread.
```
2025-01-25 22:20:54,418 [http-nio-9876-exec-2] TRACE                    o.z.l.Logbook :  : origin=remote type=request correlation=c7ebc44a4f3e4a6a protocol=HTTP/1.1 remote=127.0.0.1 method=GET uri=http://localhost:9876/feature-lab/someMocks/all host=localhost path=/feature-lab/someMocks/all scheme=http port=9876 headers={requestid=[532453]}
2025-01-25 22:20:54,436 [http-nio-9876-exec-2]  INFO             t.f.m.MockController : requestId=532453 : look at the MDC parameters at the beginning request
2025-01-25 22:20:54,448 [http-nio-9876-exec-2]  INFO                t.f.m.MockService : requestId=532453 : look at the MDC parameters in Service ThreadLocal: MonoDoFinally
2025-01-25 22:20:54,705 [reactor-http-nio-3] TRACE                    o.z.l.Logbook :  : origin=local type=request correlation=fe3e4a3b8a2da3df protocol=HTTP/1.1 remote=localhost method=GET uri=http://localhost:1234/mocks/all host=localhost path=/mocks/all scheme=http port=1234 headers={requestId=[532453]}
2025-01-25 22:20:54,705 [reactor-http-nio-4] TRACE                    o.z.l.Logbook :  : origin=local type=request correlation=e71496aff5414952 protocol=HTTP/1.1 remote=localhost method=GET uri=http://localhost:1234/mocks/string host=localhost path=/mocks/string scheme=http port=1234 headers={requestId=[5fee7f1b6c4e4f18aa2cc1a5622bf4ba]}
2025-01-25 22:20:54,727 [reactor-http-nio-4] TRACE                    o.z.l.Logbook :  : origin=remote type=response correlation=e71496aff5414952 duration=136 protocol=HTTP/1.1 status=200 body="Hello world!"
2025-01-25 22:20:54,728 [reactor-http-nio-3] TRACE                    o.z.l.Logbook :  : origin=remote type=response correlation=fe3e4a3b8a2da3df duration=260 protocol=HTTP/1.1 status=200 body=[{"name":"Ivan","salary":2000},{"name":"Alexsandr","salary":4500}]
2025-01-25 22:20:54,758 [reactor-http-nio-3]  INFO                t.f.m.MockService :  : look at the MDC parameters in Reactive Thread: "Hello world!"
2025-01-25 22:20:54,759 [http-nio-9876-exec-2]  INFO             t.f.m.MockController : requestId=532453 : look at the MDC parameters at the beginning request
2025-01-25 22:20:54,773 [http-nio-9876-exec-2] TRACE                    o.z.l.Logbook :  : origin=local type=response correlation=c7ebc44a4f3e4a6a duration=359 protocol=HTTP/1.1 status=200 headers={requestId=[532453]} body=[{"salary":2000,"name":"Ivan"},{"salary":4500,"name":"Alexsandr"}]
```

To transfer the MDC between ThreadLocal and the Reactive Thread, you need to:
1. Include dependency `logbook-spring-boot-webflux-autoconfigure`
2. Check the filter setup in [[### WebClientConfig]]]

### WebClientConfig

First in the chain is the first added filter (mdcClearFilter). 
It takes control, does nothing when processing the request, and passes the request on to the next filter by calling next.exchange(...).

Second in the chain is the second added filter (LogbookExchangeFilterFunction). 
It also takes control, processes the request under the hood, and then passes it to the next filter by calling next.exchange(...).

Once the first and second filters have called next.exchange(...), control moves to the third filter (mdcCopyFilter). 
This filter also returns a Mono/Flux, finalizing the reactive chain.

As a result, there is a single reactive sequence in which each filter adds its own logic to handle the request/response.
```
        /**
         * mdcClearFilter exchange(...) {
         *  return Filter LogbookExchangeFilterFunction: exchange(...) {
         *     return Filter mdcCopyFilter: exchange(...) {
         *         return realExchange(...)
         *             .doOnNext(...)  <-- doOnNext from mdcCopyFilter
         *     }
         *     .flatMap(...)         <-- flatMap from LogbookExchangeFilterFunction (logging the request and response)
         * }
         * .doFinally(...)            // <-- doFinally from mdcClearFilter
         */
```


When the actual request occurs, the entire chain “assembles” itself:

The request is executed (inside realExchange(...)).
As soon as a response is received, onNext() is passed up the chain.
The code inside mdcCopyFilter (.doOnNext(...)) runs first, since it is “attached” closer to the source (the real HTTP call).
After that, the result continues upward and reaches .flatMap(...) in the LogbookExchangeFilterFunction.
In flatMap(...), a new (or the same) Mono is returned, passing the result further along.
Finally, doFinally in mdcClearFilter executes, and the MDC context is cleared for that reactive flow.

As a result, we've ensured that the MDC is logged correctly in all threads.
```
2025-01-25 22:34:27,536 [http-nio-9876-exec-1] TRACE                    o.z.l.Logbook :  : origin=remote type=request correlation=e7ae9d7d56a97d02 protocol=HTTP/1.1 remote=127.0.0.1 method=GET uri=http://localhost:9876/feature-lab/someMocks/all host=localhost path=/feature-lab/someMocks/all scheme=http port=9876 headers={requestid=[532453]}
2025-01-25 22:34:27,556 [http-nio-9876-exec-1]  INFO             t.f.m.MockController : requestId=532453 : look at the MDC parameters at the beginning request
2025-01-25 22:34:27,569 [http-nio-9876-exec-1]  INFO                t.f.m.MockService : requestId=532453 : look at the MDC parameters in Service ThreadLocal: MonoDoFinally
2025-01-25 22:34:27,836 [reactor-http-nio-3] TRACE                    o.z.l.Logbook : requestId=532453 : origin=local type=request correlation=efe5d0331daf42af protocol=HTTP/1.1 remote=localhost method=GET uri=http://localhost:1234/mocks/all host=localhost path=/mocks/all scheme=http port=1234 headers={requestId=[532453]}
2025-01-25 22:34:27,836 [reactor-http-nio-4] TRACE                    o.z.l.Logbook : requestId=532453 : origin=local type=request correlation=8537a3d0d97601e7 protocol=HTTP/1.1 remote=localhost method=GET uri=http://localhost:1234/mocks/string host=localhost path=/mocks/string scheme=http port=1234 headers={requestId=[d43801d341b546cb9c02b5d0e833662b]}
2025-01-25 22:34:27,863 [reactor-http-nio-4] TRACE                    o.z.l.Logbook : requestId=532453 : origin=remote type=response correlation=8537a3d0d97601e7 duration=151 protocol=HTTP/1.1 status=200 body="Hello world!"
2025-01-25 22:34:27,864 [reactor-http-nio-3] TRACE                    o.z.l.Logbook : requestId=532453 : origin=remote type=response correlation=efe5d0331daf42af duration=271 protocol=HTTP/1.1 status=200 body=[{"name":"Ivan","salary":2000},{"name":"Alexsandr","salary":4500}]
2025-01-25 22:34:27,897 [reactor-http-nio-3]  INFO                t.f.m.MockService : requestId=532453 : look at the MDC parameters in Reactive Thread: "Hello world!"
2025-01-25 22:34:27,898 [http-nio-9876-exec-1]  INFO             t.f.m.MockController : requestId=532453 : look at the MDC parameters at the beginning request
2025-01-25 22:34:27,913 [http-nio-9876-exec-1] TRACE                    o.z.l.Logbook :  : origin=local type=response correlation=e7ae9d7d56a97d02 duration=380 protocol=HTTP/1.1 status=200 headers={requestId=[532453]} body=[{"salary":2000,"name":"Ivan"},{"salary":4500,"name":"Alexsandr"}]
```