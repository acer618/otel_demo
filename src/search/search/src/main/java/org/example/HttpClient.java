
/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.incubator.propagation.ExtendedContextPropagators;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import org.example.propagation.B3Propagator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;


public final class HttpClient {

  // it's important to initialize the OpenTelemetry SDK as early in your applications lifecycle as
  // possible.
  private static final OpenTelemetry openTelemetry = ExampleConfiguration.initOpenTelemetry();

  private static final Tracer tracer =
      openTelemetry.getTracer("io.opentelemetry.example.http.HttpClient");
  private static final int port = Integer.parseInt(System.getProperty("SEARCH_PORT"));

  private void makeRequest() throws IOException, URISyntaxException {
    URL url = new URL("http://127.0.0.1:" + port + "/api/businesses");
    HttpURLConnection con = (HttpURLConnection) url.openConnection();

    int status = 0;
    StringBuilder content = new StringBuilder();

    // Name convention for the Span is not yet defined.
    // See: https://github.com/open-telemetry/opentelemetry-specification/issues/270
    Span span = tracer.spanBuilder("/api/businesses").setSpanKind(SpanKind.CLIENT).startSpan();
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
      }
    } finally {
      System.out.println(span);
      span.end();
    }

    // Output the result of the request
    System.out.println("Response Code: " + status);
    System.out.println("Response Msg: " + content);
  }

  /**
   * Main method to run the example.
   *
   * @param args It is not required.
   */
  public static void main(String[] args) {
    HttpClient httpClient = new HttpClient();

    // Perform request every 5s
    Thread t =
        new Thread(
            () -> {
              boolean flag = true;
              while (flag) {
                try {
                  httpClient.makeRequest();
                  Thread.sleep(5000);
                  flag = false;
                } catch (Exception e) {
                  System.out.println(e.getMessage());
                }
              }
            });
    t.start();
  }
}

