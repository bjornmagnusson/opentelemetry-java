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

package io.opentelemetry.opentracingshim.testbed.baggagehandling;

import static io.opentelemetry.opentracingshim.testbed.TestUtils.createTracerShim;
import static org.junit.Assert.assertEquals;

import io.opentelemetry.exporters.inmemory.InMemorySpanExporter;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public final class BaggageHandlingTest {
  private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
  private final Tracer tracer = createTracerShim(exporter);
  private final ExecutorService executor = Executors.newCachedThreadPool();

  @Test
  public void test_multithreaded() throws Exception {
    final Span span = tracer.buildSpan("one").start();
    span.setBaggageItem("key1", "value1");

    Future<?> f =
        executor.submit(
            new Runnable() {
              @Override
              public void run() {
                /* Override the previous value... */
                span.setBaggageItem("key1", "value2");

                /* add a new baggage item... */
                span.setBaggageItem("newkey", "newvalue");

                /* and finish the Span. */
                span.finish();
              }
            });

    /* Single call, no need to use await() */
    f.get(5, TimeUnit.SECONDS);

    assertEquals(1, exporter.getFinishedSpanItems().size());
    assertEquals(span.getBaggageItem("key1"), "value2");
    assertEquals(span.getBaggageItem("newkey"), "newvalue");
  }
}
