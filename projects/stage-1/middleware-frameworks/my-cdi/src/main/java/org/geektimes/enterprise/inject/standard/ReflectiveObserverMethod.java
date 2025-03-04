/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.geektimes.enterprise.inject.standard;

import org.geektimes.enterprise.inject.standard.event.ObserverMethodParameter;

import javax.enterprise.event.Observes;
import javax.enterprise.event.ObservesAsync;
import javax.enterprise.event.Reception;
import javax.enterprise.event.TransactionPhase;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.DefinitionException;
import javax.enterprise.inject.spi.EventContext;
import javax.enterprise.inject.spi.ObserverMethod;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import static java.lang.String.format;
import static org.geektimes.commons.function.ThrowableAction.execute;
import static org.geektimes.commons.lang.util.ArrayUtils.asArray;
import static org.geektimes.commons.reflect.util.MemberUtils.isStatic;
import static org.geektimes.enterprise.inject.util.Events.getObservedParameter;
import static org.geektimes.enterprise.inject.util.Events.getObserverMethodParameters;
import static org.geektimes.enterprise.inject.util.Qualifiers.getQualifiers;

/**
 * {@link ObserverMethod} based on Java Reflection.
 *
 * @param <T>
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 */
public class ReflectiveObserverMethod<T> implements ObserverMethod<T> {

    private static final ThreadLocal<Object> beanInstanceThreadLocal = new ThreadLocal<>();

    private final Object beanInstance;

    private final Method method;

    private final Instance<Object> instance;

    private final List<ObserverMethodParameter> observerMethodParameters;

    private final ObserverMethodParameter observedParameter;

    private final Reception reception;

    private final TransactionPhase transactionPhase;

    private final boolean async;

    public ReflectiveObserverMethod(Object beanInstance, Method method, Instance<Object> instance) {
        this.beanInstance = beanInstance;
        this.method = method;
        this.instance = instance;
        this.observerMethodParameters = getObserverMethodParameters(method);
        this.observedParameter = getObservedParameter(observerMethodParameters);
        Observes observes = observedParameter.getAnnotation(Observes.class);
        if (observes != null) {
            async = false;
            this.reception = observes.notifyObserver();
            this.transactionPhase = observes.during();
        } else {
            ObservesAsync observesAsync = observedParameter.getAnnotation(ObservesAsync.class);
            async = true;
            this.reception = observesAsync.notifyObserver();
            this.transactionPhase = null;
        }
    }

    @Override
    public Class<?> getBeanClass() {
        return method.getDeclaringClass();
    }

    @Override
    public Type getObservedType() {
        return observedParameter.getParameterizedType();
    }

    @Override
    public Set<Annotation> getObservedQualifiers() {
        return getQualifiers(observedParameter);
    }

    @Override
    public Reception getReception() {
        return reception;
    }

    @Override
    public TransactionPhase getTransactionPhase() {
        return transactionPhase;
    }

    @Override
    public void notify(T event) {

    }

    @Override
    public void notify(EventContext<T> eventContext) {
        int size = observerMethodParameters.size();
        Object[] parameterValues = new Object[size];
        for (int i = 0; i < size; i++) {
            ObserverMethodParameter parameter = observerMethodParameters.get(i);
            parameterValues[i] = resolveParameterValue(eventContext, parameter);
        }
        Object object = isStatic(method) ? null : beanInstance;
        // set the bean instance into ThreadLocal
        setBeanInstance(beanInstance);
        // invoke the observer method
        try {
            execute(() -> method.invoke(object, parameterValues));
        } finally {
            clearBeanInstance();
        }
    }

    private Object resolveParameterValue(EventContext<T> eventContext, ObserverMethodParameter parameter) {
        if (parameter.isObserved()) {
            return eventContext.getEvent();
        } else if (parameter.isMetadata()) {
            return eventContext.getMetadata();
        } else if (parameter.isInjected()) {
            Class<?> parameterType = parameter.getType();
            Set<Annotation> qualifiers = getQualifiers(parameter);
            Instance<?> targetInstance = instance.select(parameterType, asArray(qualifiers, Annotation.class));
            if (targetInstance.isUnsatisfied()) {
                String message = format("The injected instance can't be resolved from parameter[%s] with qualifiers[%s]",
                        parameter.getParameter(), qualifiers);
                throw new UnsatisfiedResolutionException(message);
            }
            return targetInstance.get();
        }
        String message = format("The observer method[%s] can't resolve the parameter[%s]",
                method, parameter.getParameter());
        throw new DefinitionException(message);
    }

    @Override
    public boolean isAsync() {
        return async;
    }

    @Override
    public String toString() {
        return "ReflectiveObserverMethod{" +
                "instance=" + beanInstance +
                ", method=" + method +
                ", parameters=" + observerMethodParameters +
                ", reception=" + reception +
                ", transactionPhase=" + transactionPhase +
                ", async=" + async +
                '}';
    }

    /**
     * Get the bean instance from observer method
     *
     * @return non-null
     */
    public static Object getBeanInstance() {
        return beanInstanceThreadLocal.get();
    }

    private static void setBeanInstance(Object beanInstance) {
        beanInstanceThreadLocal.set(beanInstance);
    }

    private static void clearBeanInstance() {
        beanInstanceThreadLocal.remove();
    }
}
