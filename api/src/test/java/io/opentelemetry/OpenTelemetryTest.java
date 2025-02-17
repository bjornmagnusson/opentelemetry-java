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

package io.opentelemetry;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertTrue;

import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.BinaryFormat;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.distributedcontext.DefaultDistributedContextManager;
import io.opentelemetry.distributedcontext.DistributedContext;
import io.opentelemetry.distributedcontext.DistributedContextManager;
import io.opentelemetry.distributedcontext.spi.DistributedContextManagerProvider;
import io.opentelemetry.metrics.CounterDouble;
import io.opentelemetry.metrics.CounterLong;
import io.opentelemetry.metrics.DefaultMeter;
import io.opentelemetry.metrics.GaugeDouble;
import io.opentelemetry.metrics.GaugeLong;
import io.opentelemetry.metrics.MeasureBatchRecorder;
import io.opentelemetry.metrics.MeasureDouble;
import io.opentelemetry.metrics.MeasureLong;
import io.opentelemetry.metrics.Meter;
import io.opentelemetry.metrics.ObserverDouble;
import io.opentelemetry.metrics.ObserverLong;
import io.opentelemetry.metrics.spi.MeterProvider;
import io.opentelemetry.trace.DefaultTracer;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import io.opentelemetry.trace.Tracer;
import io.opentelemetry.trace.TracerFactory;
import io.opentelemetry.trace.spi.TracerFactoryProvider;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import javax.annotation.Nullable;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class OpenTelemetryTest {

  @Rule public final ExpectedException thrown = ExpectedException.none();

  @BeforeClass
  public static void beforeClass() {
    OpenTelemetry.reset();
  }

  @After
  public void after() {
    OpenTelemetry.reset();
    System.clearProperty(TracerFactoryProvider.class.getName());
    System.clearProperty(MeterProvider.class.getName());
    System.clearProperty(DistributedContextManagerProvider.class.getName());
  }

  @Test
  public void testDefault() {
    assertThat(OpenTelemetry.getTracerFactory().get("testTracer"))
        .isInstanceOf(DefaultTracer.getInstance().getClass());
    assertThat(OpenTelemetry.getTracerFactory().get("testTracer"))
        .isEqualTo(OpenTelemetry.getTracerFactory().get("testTracer"));
    assertThat(OpenTelemetry.getMeter()).isInstanceOf(DefaultMeter.getInstance().getClass());
    assertThat(OpenTelemetry.getMeter()).isEqualTo(OpenTelemetry.getMeter());
    assertThat(OpenTelemetry.getDistributedContextManager())
        .isInstanceOf(DefaultDistributedContextManager.getInstance().getClass());
    assertThat(OpenTelemetry.getDistributedContextManager())
        .isEqualTo(OpenTelemetry.getDistributedContextManager());
  }

  @Test
  public void testTracerLoadArbitrary() throws IOException {
    File serviceFile =
        createService(
            TracerFactoryProvider.class, FirstTracerFactory.class, SecondTracerFactory.class);
    try {
      assertTrue(
          (OpenTelemetry.getTracerFactory() instanceof FirstTracerFactory)
              || (OpenTelemetry.getTracerFactory() instanceof SecondTracerFactory));
    } finally {
      serviceFile.delete();
    }
  }

  @Test
  public void testTracerSystemProperty() throws IOException {
    File serviceFile =
        createService(
            TracerFactoryProvider.class, FirstTracerFactory.class, SecondTracerFactory.class);
    System.setProperty(TracerFactoryProvider.class.getName(), SecondTracerFactory.class.getName());
    try {
      assertThat(OpenTelemetry.getTracerFactory()).isInstanceOf(SecondTracerFactory.class);
    } finally {
      serviceFile.delete();
    }
  }

  @Test
  public void testTracerNotFound() {
    System.setProperty(TracerFactoryProvider.class.getName(), "io.does.not.exists");
    thrown.expect(IllegalStateException.class);
    OpenTelemetry.getTracerFactory().get("testTracer");
  }

  @Test
  public void testMeterLoadArbitrary() throws IOException {
    File serviceFile = createService(MeterProvider.class, FirstMeter.class, SecondMeter.class);
    try {
      assertTrue(
          (OpenTelemetry.getMeter() instanceof FirstMeter)
              || (OpenTelemetry.getMeter() instanceof SecondMeter));
      assertThat(OpenTelemetry.getMeter()).isEqualTo(OpenTelemetry.getMeter());
    } finally {
      serviceFile.delete();
    }
  }

  @Test
  public void testMeterSystemProperty() throws IOException {
    File serviceFile = createService(MeterProvider.class, FirstMeter.class, SecondMeter.class);
    System.setProperty(MeterProvider.class.getName(), SecondMeter.class.getName());
    try {
      assertThat(OpenTelemetry.getMeter()).isInstanceOf(SecondMeter.class);
      assertThat(OpenTelemetry.getMeter()).isEqualTo(OpenTelemetry.getMeter());
    } finally {
      serviceFile.delete();
    }
  }

  @Test
  public void testMeterNotFound() {
    System.setProperty(MeterProvider.class.getName(), "io.does.not.exists");
    thrown.expect(IllegalStateException.class);
    OpenTelemetry.getMeter();
  }

  @Test
  public void testDistributedContextManagerLoadArbitrary() throws IOException {
    File serviceFile =
        createService(
            DistributedContextManagerProvider.class,
            FirstDistributedContextManager.class,
            SecondDistributedContextManager.class);
    try {
      assertTrue(
          (OpenTelemetry.getDistributedContextManager() instanceof FirstDistributedContextManager)
              || (OpenTelemetry.getDistributedContextManager()
                  instanceof SecondDistributedContextManager));
      assertThat(OpenTelemetry.getDistributedContextManager())
          .isEqualTo(OpenTelemetry.getDistributedContextManager());
    } finally {
      serviceFile.delete();
    }
  }

  @Test
  public void testDistributedContextManagerSystemProperty() throws IOException {
    File serviceFile =
        createService(
            DistributedContextManagerProvider.class,
            FirstDistributedContextManager.class,
            SecondDistributedContextManager.class);
    System.setProperty(
        DistributedContextManagerProvider.class.getName(),
        SecondDistributedContextManager.class.getName());
    try {
      assertThat(OpenTelemetry.getDistributedContextManager())
          .isInstanceOf(SecondDistributedContextManager.class);
      assertThat(OpenTelemetry.getDistributedContextManager())
          .isEqualTo(OpenTelemetry.getDistributedContextManager());
    } finally {
      serviceFile.delete();
    }
  }

  @Test
  public void testDistributedContextManagerNotFound() {
    System.setProperty(DistributedContextManagerProvider.class.getName(), "io.does.not.exists");
    thrown.expect(IllegalStateException.class);
    OpenTelemetry.getDistributedContextManager();
  }

  private static File createService(Class<?> service, Class<?>... impls) throws IOException {
    URL location = Tracer.class.getProtectionDomain().getCodeSource().getLocation();
    File file = new File(location.getPath() + "META-INF/services/" + service.getName());
    file.getParentFile().mkdirs();

    @SuppressWarnings("DefaultCharset")
    Writer output = new FileWriter(file);
    for (Class<?> impl : impls) {
      output.write(impl.getName());
      output.write(System.getProperty("line.separator"));
    }
    output.close();
    return file;
  }

  public static class SecondTracerFactory extends FirstTracerFactory {
    @Override
    public Tracer get(String instrumentationName) {
      return new SecondTracerFactory();
    }

    @Override
    public Tracer get(String instrumentationName, String instrumentationVersion) {
      return get(instrumentationName);
    }

    @Override
    public TracerFactory create() {
      return new SecondTracerFactory();
    }
  }

  public static class FirstTracerFactory implements Tracer, TracerFactory, TracerFactoryProvider {
    @Override
    public Tracer get(String instrumentationName) {
      return new FirstTracerFactory();
    }

    @Override
    public Tracer get(String instrumentationName, String instrumentationVersion) {
      return get(instrumentationName);
    }

    @Nullable
    @Override
    public Span getCurrentSpan() {
      return null;
    }

    @Nullable
    @Override
    public Scope withSpan(Span span) {
      return null;
    }

    @Nullable
    @Override
    public Span.Builder spanBuilder(String spanName) {
      return null;
    }

    @Nullable
    @Override
    public BinaryFormat<SpanContext> getBinaryFormat() {
      return null;
    }

    @Nullable
    @Override
    public HttpTextFormat<SpanContext> getHttpTextFormat() {
      return null;
    }

    @Override
    public TracerFactory create() {
      return new FirstTracerFactory();
    }
  }

  public static class SecondMeter extends FirstMeter {
    @Override
    public Meter create() {
      return new SecondMeter();
    }
  }

  public static class FirstMeter implements Meter, MeterProvider {
    @Override
    public Meter create() {
      return new FirstMeter();
    }

    @Nullable
    @Override
    public GaugeLong.Builder gaugeLongBuilder(String name) {
      return null;
    }

    @Nullable
    @Override
    public GaugeDouble.Builder gaugeDoubleBuilder(String name) {
      return null;
    }

    @Nullable
    @Override
    public CounterDouble.Builder counterDoubleBuilder(String name) {
      return null;
    }

    @Nullable
    @Override
    public CounterLong.Builder counterLongBuilder(String name) {
      return null;
    }

    @Nullable
    @Override
    public MeasureDouble.Builder measureDoubleBuilder(String name) {
      return null;
    }

    @Nullable
    @Override
    public MeasureLong.Builder measureLongBuilder(String name) {
      return null;
    }

    @Nullable
    @Override
    public ObserverDouble.Builder observerDoubleBuilder(String name) {
      return null;
    }

    @Nullable
    @Override
    public ObserverLong.Builder observerLongBuilder(String name) {
      return null;
    }

    @Nullable
    @Override
    public MeasureBatchRecorder newMeasureBatchRecorder() {
      return null;
    }
  }

  public static class SecondDistributedContextManager extends FirstDistributedContextManager {
    @Override
    public DistributedContextManager create() {
      return new SecondDistributedContextManager();
    }
  }

  public static class FirstDistributedContextManager
      implements DistributedContextManager, DistributedContextManagerProvider {
    @Override
    public DistributedContextManager create() {
      return new FirstDistributedContextManager();
    }

    @Nullable
    @Override
    public DistributedContext getCurrentContext() {
      return null;
    }

    @Nullable
    @Override
    public DistributedContext.Builder contextBuilder() {
      return null;
    }

    @Nullable
    @Override
    public Scope withContext(DistributedContext distContext) {
      return null;
    }

    @Nullable
    @Override
    public BinaryFormat<DistributedContext> getBinaryFormat() {
      return null;
    }

    @Nullable
    @Override
    public HttpTextFormat<DistributedContext> getHttpTextFormat() {
      return null;
    }
  }
}
