/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.incubator.propagation.ExtendedContextPropagators;
import io.opentelemetry.api.incubator.trace.ExtendedSpanBuilder;
import io.opentelemetry.api.trace.*;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.example.propagation.B3Propagator;

import javax.annotation.Nullable;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

public final class HttpServer {
  // It's important to initialize your OpenTelemetry SDK as early in your application's lifecycle as
  // possible.
  private static final OpenTelemetry openTelemetry = ExampleConfiguration.initOpenTelemetry();
  private static final Tracer tracer =
      openTelemetry.getTracer("io.opentelemetry.example.http.HttpServer");

  private static final int port = Integer.parseInt(System.getProperty("SEARCH_PORT"));
  private static final int business_search_port = Integer.parseInt(System.getProperty("BUSINESS_SEARCH_PORT"));
  //private static final int ad_delivery_port = Integer.parseInt(System.getProperty("AD_DELIVERY_PORT"));


  private static String CURRENT_PARENT_ID = "";
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

  public static String get_current_parent_id() {
      return CURRENT_PARENT_ID;
  }

  private HttpServer(int port) throws IOException {
    server = com.sun.net.httpserver.HttpServer.create(new InetSocketAddress(port), 0);
    // Test urls
    server.createContext("/api/businesses", new SearchHandler());
    server.start();
    System.out.println("Server ready on http://127.0.0.1:" + port);
  }

  private static class SearchHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {

        Map<String, String> headersMap = exchange.getRequestHeaders().entrySet().stream()
              .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().get(0)));
        System.out.println(headersMap);
        System.out.println();

        //Solve this in a better way. Why do all headers start with an uppercase character?
        if (headersMap.containsKey("Traceparent")) {
          headersMap.put("traceparent", headersMap.get("Traceparent"));
        }

        ContextPropagators contextPropagators = ContextPropagators.create(TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), B3Propagator.injectingMultiHeaders()));
        Context context = contextPropagators.getTextMapPropagator().extract(Context.current(), headersMap, TEXT_MAP_GETTER );

        try {
            ((ExtendedSpanBuilder)
                    tracer.spanBuilder("GET /api/businesses")
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
                      span.setAttribute("http.target", "/businesses");

                      getBusinesses(exchange, span);
                      System.out.println("SERVER span:" + span);
                      System.out.println();
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void getBusinesses(HttpExchange exchange, Span span) throws IOException {
        // Generate an Event
        span.addEvent("Start getting businesses");

        String response = "{}";
        try {
            response = makeRequest("http://127.0.0.1:" + business_search_port + "/businesses");
        } catch (URISyntaxException e) {
            System.out.println(e.getMessage());
        }
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(Charset.defaultCharset()));
        os.close();
        System.out.println("Served Client: " + exchange.getRemoteAddress());

        // Generate an Event with an attribute
        Attributes eventAttributes = Attributes.of(stringKey("answer"), response);
        span.addEvent("Finish Processing", eventAttributes);
    }

    private void getAds(HttpExchange exchange, Span span) throws IOException, URISyntaxException {
        // Generate an Event
        span.addEvent("Start getting Ads");

        String response = "";
        try {
            response = makeRequest("http://127.0.0.1:" + "8083" + "/ads");
        } catch (URISyntaxException e) {
            System.out.println(e.getMessage());
        }

        //exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes(Charset.defaultCharset()));
        os.close();
        System.out.println("Served Client: " + exchange.getRemoteAddress());

        // Generate an Event with an attribute
        Attributes eventAttributes = Attributes.of(stringKey("answer"), response);
        span.addEvent("Finish Processing", eventAttributes);
      }

      private String makeRequest(String urlString) throws IOException, URISyntaxException {
          URL url = new URL(urlString);
          HttpURLConnection con = (HttpURLConnection) url.openConnection();

          int status = 0;
          StringBuilder content = new StringBuilder();

          CURRENT_PARENT_ID = Span.fromContext(Context.current()).getSpanContext().getSpanId();
          Span span = tracer.spanBuilder("getAds").setSpanKind(SpanKind.CLIENT).startSpan();

          try (Scope scope = span.makeCurrent()) {
              span.setAttribute("http.method", "GET");
              span.setAttribute("component", "http");

              URI uri = url.toURI();
              url =
                      new URI(
                              uri.getScheme(),
                              null,
                              uri.getHost(),
                              uri.getPort(),
                              uri.getPath(),
                              uri.getQuery(),
                              uri.getFragment())
                              .toURL();

              span.setAttribute("url.full", url.toString());
              ContextPropagators contextPropagators =
                      ContextPropagators.create(
                              TextMapPropagator.composite(W3CTraceContextPropagator.getInstance(), B3Propagator.injectingMultiHeaders()));

              ExtendedContextPropagators.getTextMapPropagationContext(contextPropagators)
                      .forEach(con::setRequestProperty);

              try {
                  // Process the request
                  con.setRequestMethod("GET");
                  status = con.getResponseCode();

                  BufferedReader in =
                          new BufferedReader(
                                  new InputStreamReader(con.getInputStream(), Charset.defaultCharset()));
                  String inputLine;
                  while ((inputLine = in.readLine()) != null) {
                      content.append(inputLine);
                  }
                  in.close();

              } catch (Exception e) {
                  span.setStatus(StatusCode.ERROR, "HTTP Code: " + status);
                  throw e;
              }
          } finally {
              System.out.println("CLIENT span:");
              System.out.println(span);
              System.out.println();
              span.end();
          }

          // Output the result of the request
          System.out.println("Response Code: " + status);
          System.out.println("Response Msg: " + content);
          return content.toString();
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
