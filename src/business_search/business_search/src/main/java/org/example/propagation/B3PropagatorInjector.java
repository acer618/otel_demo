/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.example.propagation;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;
import java.util.Collection;

@Immutable
interface B3PropagatorInjector {
  <C> void inject(Context context, @Nullable C carrier, TextMapSetter<C> setter);

  Collection<String> fields();
}
