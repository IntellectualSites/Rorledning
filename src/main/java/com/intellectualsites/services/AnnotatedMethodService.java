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

import com.google.common.base.Objects;
import com.intellectualsites.services.annotations.Order;
import com.intellectualsites.services.types.Service;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

class AnnotatedMethodService<Context, Result> implements Service<Context, Result> {

    private final ExecutionOrder executionOrder;
    private final MethodHandle methodHandle;
    private final Method method;
    private final Object instance;

    AnnotatedMethodService(@Nonnull final Object instance, @Nonnull final Method method)
        throws Exception {
      ExecutionOrder executionOrder = ExecutionOrder.SOON;
      try {
        final Order order = method.getAnnotation(Order.class);
        if (order != null) {
          executionOrder = order.value();
        }
      } catch (final Exception ignored) {
      }
      this.instance = instance;
      this.executionOrder = executionOrder;
      method.setAccessible(true);
      this.methodHandle = MethodHandles.lookup().unreflect(method);
      this.method = method;
    }

  @Nullable
  @Override
  @SuppressWarnings("unchecked")
  public Result handle(@Nonnull final Context context) {
    try {
      return (Result) this.methodHandle.invoke(this.instance, context);
    } catch (final Throwable throwable) {
      new IllegalStateException(String
          .format("Failed to call method service implementation '%s' in class '%s'",
              method.getName(), instance.getClass().getCanonicalName()), throwable)
          .printStackTrace();
    }
    return null;
  }

  @Nonnull
  @Override
  public ExecutionOrder order() {
    return this.executionOrder;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final AnnotatedMethodService<?, ?> that = (AnnotatedMethodService<?, ?>) o;
    return Objects.equal(this.methodHandle, that.methodHandle);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.methodHandle);
  }

}
