/*
 * Copyright 2019, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.sdk.trace;

import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.BinaryFormat;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.sdk.trace.config.TraceConfig;
import io.opentelemetry.trace.DefaultTracer;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.propagation.BinaryTraceContext;
import io.opentelemetry.trace.propagation.HttpTraceContext;
import io.opentelemetry.trace.unsafe.ContextUtils;

/** {@link TracerSdk} is SDK implementation of {@link Tracer}. */
public class TracerSdk implements Tracer {
  private static final BinaryFormat<SpanContext> BINARY_FORMAT = new BinaryTraceContext();
  private static final HttpTextFormat<SpanContext> HTTP_TEXT_FORMAT = new HttpTraceContext();
  private final TracerSharedState sharedState;

  /** TODO: Remove this when all tests are changed to use the factory. */
  public TracerSdk() {
    this.sharedState = new TracerSharedState();
  }

  TracerSdk(TracerSharedState sharedState) {
    this.sharedState = sharedState;
  }

  @Override
  public Span getCurrentSpan() {
    return ContextUtils.getValue();
  }

  @Override
  public Scope withSpan(Span span) {
    return ContextUtils.withSpan(span);
  }

  @Override
  public Span.Builder spanBuilder(String spanName) {
    if (sharedState.isStopped()) {
      return DefaultTracer.getInstance().spanBuilder(spanName);
    }
    return new SpanBuilderSdk(
        spanName,
        sharedState.getActiveSpanProcessor(),
        sharedState.getActiveTraceConfig(),
        sharedState.getResource(),
        sharedState.getIdsGenerator(),
        sharedState.getClock());
  }

  @Override
  public BinaryFormat<SpanContext> getBinaryFormat() {
    return BINARY_FORMAT;
  }

  @Override
  public HttpTextFormat<SpanContext> getHttpTextFormat() {
    return HTTP_TEXT_FORMAT;
  }

  /** TODO: Remove this when all tests are changed to use the factory. */
  public TraceConfig getActiveTraceConfig() {
    return sharedState.getActiveTraceConfig();
  }

  /** TODO: Remove this when all tests are changed to use the factory. */
  public void updateActiveTraceConfig(TraceConfig traceConfig) {
    sharedState.updateActiveTraceConfig(traceConfig);
  }

  /** TODO: Remove this when all tests are changed to use the factory. */
  public void addSpanProcessor(SpanProcessor spanProcessor) {
    sharedState.addSpanProcessor(spanProcessor);
  }

  /** TODO: Remove this when all tests are changed to use the factory. */
  public void shutdown() {
    sharedState.stop();
  }
}
