//
// MIT License
//
// Copyright (c) 2020 IntellectualSites
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package com.intellectualsites.services;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a request that can be fulfilled by one, or multiple services, for one or
 * more objects
 *
 * @param <Context> Context/Request type
 * @param <Result>  Result type
 */
public abstract class ChunkedRequestContext<Context, Result> {

    private final Object lock = new Object();
    private final List<Context> requests;
    private final Map<Context, Result> results;

    /**
     * Initialize a new request
     *
     * @param requests Request contexts
     */
    protected ChunkedRequestContext(@Nonnull final Collection<Context> requests) {
        this.requests = new ArrayList<>(requests);
        this.results = new HashMap<>(requests.size());
    }

  /**
   * Get a view of the (currently) available results
   *
   * @return Unmodifiable map of results
   */
  @Nonnull
  public final Map<Context, Result> getAvailableResults() {
    synchronized (this.lock) {
      return Collections.unmodifiableMap(this.results);
    }
  }

  /**
   * Get all remaining requests
   *
   * @return Unmodifiable list of remaining requests
   */
  @Nonnull
  public final List<Context> getRemaining() {
    synchronized (this.lock) {
      return Collections.unmodifiableList(this.requests);
    }
  }

    /**
     * Store a result for a specific context
     *
     * @param context Context
     * @param result  Result
     */
    public final void storeResult(@Nonnull final Context context, @Nonnull final Result result) {
        synchronized (this.lock) {
            this.results.put(context, result);
            this.requests.remove(context);
        }
    }

  /**
   * Check if the request has been completed
   *
   * @return {@code true} if the request has been completed, {@code false} if not
   */
    public final boolean isCompleted() {
        synchronized (this.lock) {
            return this.requests.isEmpty();
        }
    }

}
