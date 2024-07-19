/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.propagation;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

import static org.example.propagation.B3Propagator.*;

@Immutable
final class B3PropagatorInjectorMultipleHeaders implements B3PropagatorInjector {
  private static final Collection<String> FIELDS =
      Collections.unmodifiableList(Arrays.asList(TRACE_ID_HEADER, PARENT_SPAN_ID_HEADER, SPAN_ID_HEADER, SAMPLED_HEADER));

  @Override
  public <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter) {
    if (context == null) {
      return;
    }
    if (setter == null) {
      return;
    }

    SpanContext spanContext = Span.fromContext(context).getSpanContext();
    if (!spanContext.isValid()) {
      return;
    }

    String sampled = spanContext.isSampled() ? Common.TRUE_INT : Common.FALSE_INT;

    if (Boolean.TRUE.equals(context.get(B3Propagator.DEBUG_CONTEXT_KEY))) {
      setter.set(carrier, B3Propagator.DEBUG_HEADER, Common.TRUE_INT);
      sampled = Common.TRUE_INT;
    }

    setter.set(carrier, TRACE_ID_HEADER, spanContext.getTraceId());

    /* 
      TODO:
       Issue: SpanContext interface does not have getParentId
      https://github.com/open-telemetry/opentelemetry-java/blob/3fa57f9280ff73bc74525f0e773eaef9b2ab9489/api/all/src/main/java/io/opentelemetry/api/trace/SpanContext.java#L27
    */
    //setter.set(carrier, PARENT_SPAN_ID_HEADER, spanContext.getParentId());
    setter.set(carrier, SPAN_ID_HEADER, spanContext.getSpanId());
    setter.set(carrier, SAMPLED_HEADER, sampled);
  }

  @Override
  public Collection<String> fields() {
    return FIELDS;
  }

  @Override
  public String toString() {
    return "B3PropagatorInjectorMultipleHeaders";
  }
}
