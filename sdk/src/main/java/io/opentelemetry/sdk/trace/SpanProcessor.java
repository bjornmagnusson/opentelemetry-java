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

import io.opentelemetry.trace.Span;

/**
 * SpanProcessor is the interface {@code TracerSdk} uses to allow synchronous hooks for when a
 * {@code Span} is started or when a {@code Span} is ended.
 */
public interface SpanProcessor {
  /**
   * Called when a {@link io.opentelemetry.trace.Span} is started, if the {@link Span#isRecording()}
   * returns true.
   *
   * <p>This method is called synchronously on the execution thread, should not throw or block the
   * execution thread.
   *
   * @param span the {@code ReadableSpan} that just started.
   */
  void onStart(ReadableSpan span);

  /**
   * Called when a {@link io.opentelemetry.trace.Span} is ended, if the {@link Span#isRecording()}
   * returns true.
   *
   * <p>This method is called synchronously on the execution thread, should not throw or block the
   * execution thread.
   *
   * @param span the {@code ReadableSpan} that just ended.
   */
  // TODO: Consider checking whether the given span is processed with onStart().
  void onEnd(ReadableSpan span);

  /** Called when {@link TracerSdk#shutdown()} is called. */
  void shutdown();
}
