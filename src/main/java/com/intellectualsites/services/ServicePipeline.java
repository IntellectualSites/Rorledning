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

import com.google.common.reflect.TypeToken;
import com.intellectualsites.services.types.Service;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * Service pipeline
 */
public final class ServicePipeline {

    private final Object lock = new Object();
    private final Map<TypeToken<? extends Service<?, ?>>, ServiceRepository<?, ?>> repositories;
    private final Executor executor;

    ServicePipeline(@Nonnull final Executor executor) {
        this.repositories = new HashMap<>();
        this.executor = executor;
    }

    /**
     * Create a new {@link ServicePipelineBuilder}
     *
     * @return Builder instance
     */
    @Nonnull
    public static ServicePipelineBuilder builder() {
        return new ServicePipelineBuilder();
    }

    /**
     * Register a service type so that it is recognized by the pipeline
     *
     * @param type                  Service type
     * @param defaultImplementation Default implementation of the service. This must *always* be
     *                              supplied and will be the last service implementation that the
     *                              pipeline attempts to access. This will never be filtered out.
     * @param <Context>             Service context type
     * @param <Result>              Service result type
     * @return ServicePipeline The service pipeline instance
     */
    public <Context, Result> ServicePipeline registerServiceType(
            @Nonnull final TypeToken<? extends Service<Context, Result>> type,
            @Nonnull final Service<Context, Result> defaultImplementation) {
        synchronized (this.lock) {
            if (repositories.containsKey(type)) {
                throw new IllegalArgumentException(String
                        .format("Service of type '%s' has already been registered", type.toString()));
            }
            final ServiceRepository<Context, Result> repository = new ServiceRepository<>(type);
            repository.registerImplementation(defaultImplementation, Collections.emptyList());
            this.repositories.put(type, repository);
            return this;
        }
    }

    /**
     * Scan a given class for methods annotated with {@link com.intellectualsites.services.annotations.ServiceImplementation}
     * and register them as service implementations.
     * <p>
     * The methods should be of the form:
     * <pre>{@code
     * {@literal @}Nullable
     * {@literal @}ServiceImplementation(YourService.class)
     * public YourResult handle(YourContext) {
     *      return result;
     * }
     * }</pre>
     *
     * @param instance Instance of the class to scan
     * @param <T>      Type of the instance
     * @return Service pipeline instance
     * @throws Exception Any exceptions thrown during the registration process
     */
    @SuppressWarnings("unchecked")
    public <T> ServicePipeline registerMethods(
            @Nonnull final T instance) throws Exception {
        synchronized (this.lock) {
            final Map<? extends Service<?, ?>, TypeToken<? extends Service<?, ?>>> services =
                    AnnotatedMethodServiceFactory.INSTANCE.lookupServices(instance);
            for (final Map.Entry<? extends Service<?, ?>, TypeToken<? extends Service<?, ?>>> serviceEntry : services
                    .entrySet()) {
                final TypeToken<? extends Service<?, ?>> type = serviceEntry.getValue();
                final ServiceRepository<?, ?> repository = this.repositories.get(type);
                if (repository == null) {
                    throw new IllegalArgumentException(
                            String.format("No service registered for type '%s'", type.toString()));
                }
                repository.<Service>registerImplementation(serviceEntry.getKey(),
                                                           Collections.emptyList());
            }
        }
        return this;
    }

    /**
     * Register a service implementation for a type that is recognized by the pipeline. It is
     * important that a call to {@link #registerServiceType(TypeToken, Service)} has been made
     * beforehand, otherwise a {@link IllegalArgumentException} will be thrown
     *
     * @param type           Service type
     * @param implementation Implementation of the service
     * @param filters        Filters that will be used to determine whether or not the service gets
     *                       used
     * @param <Context>      Service context type
     * @param <Result>       Service result type
     * @return ServicePipeline The service pipeline instance
     */
    public <Context, Result> ServicePipeline registerServiceImplementation(
            @Nonnull final TypeToken<? extends Service<Context, Result>> type,
            @Nonnull final Service<Context, Result> implementation,
            @Nonnull final Collection<Predicate<Context>> filters) {
        synchronized (this.lock) {
            final ServiceRepository<Context, Result> repository = getRepository(type);
            repository.registerImplementation(implementation, filters);
        }
        return this;
    }

    /**
     * Register a service implementation for a type that is recognized by the pipeline. It is
     * important that a call to {@link #registerServiceType(TypeToken, Service)} has been made
     * beforehand, otherwise a {@link IllegalArgumentException} will be thrown
     *
     * @param type           Service type
     * @param implementation Implementation of the service
     * @param filters        Filters that will be used to determine whether or not the service gets
     *                       used
     * @param <Context>      Service context type
     * @param <Result>       Service result type
     * @return ServicePipeline The service pipeline instance
     */
    public <Context, Result> ServicePipeline registerServiceImplementation(
            @Nonnull final Class<? extends Service<Context, Result>> type,
            @Nonnull final Service<Context, Result> implementation,
            @Nonnull final Collection<Predicate<Context>> filters) {
        return registerServiceImplementation(TypeToken.of(type), implementation, filters);
    }

    /**
     * Start traversing the pipeline by providing the context that will be used to generate the
     * results
     *
     * @param context   Context
     * @param <Context> Context type
     * @return Service pumper instance
     */
    @Nonnull
    public <Context> ServicePump<Context> pump(@Nonnull final Context context) {
        return new ServicePump<>(this, context);
    }

    @SuppressWarnings("unchecked")
    @Nonnull
    <Context, Result> ServiceRepository<Context, Result> getRepository(
            @Nonnull final TypeToken<? extends Service<Context, Result>> type) {
        final ServiceRepository<Context, Result> repository =
                (ServiceRepository<Context, Result>) this.repositories.get(type);
        if (repository == null) {
            throw new IllegalArgumentException(
                    String.format("No service registered for type '%s'", type.toString()));
        }
        return repository;
    }

    /**
     * Get a collection of all the recognised service types.
     *
     * @return Returns an Immutable collection of the service types registered.
     */
    @Nonnull
    public Collection<TypeToken<? extends Service<?, ?>>> getRecognizedTypes() {
        return Collections.unmodifiableCollection(this.repositories.keySet());
    }

    /**
     * Get a collection of all the {@link TypeToken} of all implementations for a given type.
     *
     * @param type      The {@link TypeToken} of the service to get implementations for.
     * @param <Context> The context type.
     * @param <Result>  The result type.
     * @param <S>       The service type.
     * @return Returns an collection of the {@link TypeToken}s of the implementations for a given
     * service. Iterator order matches the priority when pumping contexts through the pipeline
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    public <Context, Result, S extends Service<Context, Result>> Collection<TypeToken<? extends S>> getImplementations(
            @Nonnull final TypeToken<S> type) {
        ServiceRepository<Context, Result> repository = getRepository(type);
        List<TypeToken<? extends S>> collection = new LinkedList<>();
        final LinkedList<? extends ServiceRepository<Context, Result>.ServiceWrapper<? extends Service<Context, Result>>>
                queue = repository.getQueue();
        queue.sort(null);
        Collections.reverse(queue);
        for (ServiceRepository<Context, Result>.ServiceWrapper<? extends Service<Context, Result>> wrapper : queue) {
            collection
                    .add((TypeToken<? extends S>) TypeToken.of(wrapper.getImplementation().getClass()));
        }
        return Collections.unmodifiableList(collection);
    }

    @Nonnull
    Executor getExecutor() {
        return this.executor;
    }

}
