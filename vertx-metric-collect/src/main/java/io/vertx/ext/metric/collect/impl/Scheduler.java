/*
 * Copyright 2015 Red Hat, Inc.
 *
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  and Apache License v2.0 which accompanies this distribution.
 *
 *  The Eclipse Public License is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 *
 *  The Apache License v2.0 is available at
 *  http://www.opensource.org/licenses/apache2.0.php
 *
 *  You may elect to redistribute this code under either of these licenses.
 */
package io.vertx.ext.metric.collect.impl;

import io.vertx.core.Context;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.ext.metric.collect.ExtendedMetricsOptions;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.util.concurrent.TimeUnit.*;

/**
 * Collects metrics and relay them to the sender.
 *
 * @author Thomas Segismont
 */
public class Scheduler {
  private final Vertx vertx;
  private final Handler<List<DataPoint>> sender;
  private final List<MetricSupplier> suppliers;

  private long timerId;

  /**
   * @param vertx   the {@link Vertx} managed instance
   * @param options Vertx Hawkular options
   * @param context the metric collection and sending execution context
   * @param sender  the object responsible for sending metrics to the Hawkular server
   */
  public Scheduler(Vertx vertx, ExtendedMetricsOptions options, Context context, Handler<List<DataPoint>> sender) {
    this.vertx = vertx;
    this.sender = sender;
    suppliers = new CopyOnWriteArrayList<>();
    context.runOnContext(aVoid -> {
      timerId = vertx.setPeriodic(MILLISECONDS.convert(options.getSchedule(), SECONDS), this::collectAndSend);
    });
  }

  private void collectAndSend(Long timerId) {
    suppliers.forEach(supplier -> sender.handle(supplier.collect()));
  }

  /**
   * Registers a new metric supplier.
   *
   * @param supplier an object supplying metrics to be collected
   */
  public void register(MetricSupplier supplier) {
    suppliers.add(supplier);
  }

  /**
   * Unregisters a supplier. Metrics from this supplier won't be collected any more.
   *
   * @param supplier an object supplying metrics to be collected
   */
  public void unregister(MetricSupplier supplier) {
    suppliers.remove(supplier);
  }

  /**
   * Stop collecting.
   */
  public void stop() {
    vertx.cancelTimer(timerId);
  }
}
