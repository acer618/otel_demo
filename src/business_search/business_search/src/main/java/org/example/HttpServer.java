/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.trace.ExtendedSpanBuilder;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;

import io.opentelemetry.context.propagation.TextMapGetter;
import org.example.propagation.B3Propagator;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class HttpServer {
  // It's important to initialize your OpenTelemetry SDK as early in your application's lifecycle as
  // possible.
  private static final OpenTelemetry openTelemetry = ExampleConfiguration.initOpenTelemetry();
  private static final Tracer tracer =
      openTelemetry.getTracer("io.opentelemetry.example.http.HttpServer");

  private static final int port = Integer.parseInt(System.getProperty("SERVER_PORT"));
  private final com.sun.net.httpserver.HttpServer server;

  private static final TextMapGetter<Map<String, String>> TEXT_MAP_GETTER =
          new TextMapGetter<Map<String, String>>() {
            @Override
            public Set<String> keys(Map<String, String> carrier) {
              return carrier.keySet();
            }

            @Override
            @Nullable
            public String get(@Nullable Map<String, String> carrier, String key) {
              return carrier == null ? null : carrier.get(key);
            }
          };

  private HttpServer() throws IOException {
    this(port);
  }

  private HttpServer(int port) throws IOException {
    server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
    // Test urls
    server.createContext("/", new HelloHandler());
    server.start();
    System.out.println("Server ready on http://127.0.0.1:" + port);
  }

  private static class HelloHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

      Map<String, String> headersMap = exchange.getRequestHeaders().entrySet().stream()
              .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
      System.out.println(headersMap);

      //Solve this in a better way. Why do all headers start with an uppercase character?
      if (headersMap.containsKey("Traceparent")) {
          headersMap.put("traceparent", headersMap.get("Traceparent"));
        }

        /*
            TODO:
            Verify that a Composite Propagator with W3cTracePropagator and B3Propagator will work and the order won't matter
            as long as both extractors set context the same way:

            W3cTracePropagator
            - span_id = spanid from tracestate header

            B3Propagator
            - span_id = x-b3-span-id header

         */
        ContextPropagators contextPropagators = ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), B3Propagator.injectingMultiHeaders()));
        Context context = contextPropagators.getTextMapPropagator().extract(Context.current(), headersMap, TEXT_MAP_GETTER );

      /*
         TODO: 
         The code below creates a SERVER span by invoking startSpan() here:
         https://github.com/open-telemetry/opentelemetry-java/blob/3fa57f9280ff73bc74525f0e773eaef9b2ab9489/sdk/trace/src/main/java/io/opentelemetry/sdk/trace/SdkSpanBuilder.java#L190
         Issue: Based on the otel spec a new span id is generated but based on X-B3 zipkin headers the spanId needs to be shared with the remote client span id.
         
         A new implementation for TraceProvider and SDKSpanBuilder may be needed but there might be other related classes that might have to be implemented.
         
         Alternative: Can we do this instead? (aligns with the otel spec and we might be able to see the traces fine in Jaeger)
         span_id= generateNewId()
         parent_span_id = x-b3-span-id
       */
      ((ExtendedSpanBuilder)
              ((ExtendedSpanBuilder) tracer.spanBuilder("GET /"))
                  .setParent(context)
                  .setSpanKind(SpanKind.SERVER))
          .startAndRun(
              () -> {
                // Set the Semantic Convention
                Span span = Span.current();
                span.setAttribute("component", "http");
                span.setAttribute("http.method", "GET");
                span.setAttribute("http.scheme", "http");
                span.setAttribute("http.host", "localhost:" + HttpServer.port);
                span.setAttribute("http.target", "/");
                // Process the request
                answer(exchange, span);
                System.out.println(span);
              });
    }

    private void answer(HttpExchange exchange, Span span) throws IOException {
      // Generate an Event
      System.out.println("Start Processing...");
      span.addEvent("Start Processing");

      // Process the request
      String response = "Hello World!";
      exchange.sendResponseHeaders(200, response.length());
      OutputStream os = exchange.getResponseBody();
      os.write(response.getBytes(Charset.defaultCharset()));
      os.close();
      System.out.println("Served Client: " + exchange.getRemoteAddress());

      // Generate an Event with an attribute
      Attributes eventAttributes = Attributes.of(stringKey("answer"), response);
      span.addEvent("Finish Processing", eventAttributes);
    }
  }

  private void stop() {
    server.stop(0);
  }

  /**
   * Main method to run the example.
   *
   * @param args It is not required.
   * @throws Exception Something might go wrong.
   */
  public static void main(String[] args) throws Exception {
    final HttpServer s = new HttpServer();
    // Gracefully close the server
    Runtime.getRuntime().addShutdownHook(new Thread(s::stop));
  }
}
