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

import org.geektimes.enterprise.inject.util.Beans;
import org.geektimes.enterprise.inject.util.Injections;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.CreationException;
import javax.enterprise.inject.spi.*;
import java.lang.reflect.Constructor;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.*;
import static org.geektimes.commons.collection.util.CollectionUtils.newLinkedHashSet;
import static org.geektimes.enterprise.inject.util.Beans.validateManagedBeanSpecializes;
import static org.geektimes.enterprise.inject.util.Beans.validateManagedBeanType;

/**
 * Managed {@link Bean} based on Java Reflection.
 *
 * @param <T> the type of bean
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class ManagedBean<T> extends AbstractBean<Class, T> {

    private final BeanManager beanManager;

    private final AnnotatedType annotatedType;

    private Map<AnnotatedConstructor, List<ConstructorParameterInjectionPoint>> constructorParameterInjectionPointsMap;

    private Set<FieldInjectionPoint> fieldInjectionPoints;

    private Map<AnnotatedMethod, List<MethodParameterInjectionPoint>> methodParameterInjectionPointsMap;

    public ManagedBean(BeanManager beanManager, Class<?> beanClass) {
        super(beanClass, beanClass);
        this.beanManager = beanManager;
        this.annotatedType = new ReflectiveAnnotatedType(beanClass);
    }

    @Override
    protected void validateAnnotatedElement(Class beanClass) {
        validateManagedBeanType(beanClass);
        validateManagedBeanSpecializes(beanClass);
    }

    @Override
    protected String getBeanName(Class beanClass) {
        return Beans.getBeanName(beanClass);
    }

    @Override
    public T create(CreationalContext<T> creationalContext) {
        T instance = null;

        Map<AnnotatedConstructor, List<ConstructorParameterInjectionPoint>> injectionPointsMap =
                getConstructorParameterInjectionPointsMap();

        try {
            if (injectionPointsMap.isEmpty()) { // non-argument constructor
                instance = (T) getBeanClass().newInstance();
            } else { // @Inject constructor
                // just only one Constructor annotated @Inject
                Map.Entry<AnnotatedConstructor, List<ConstructorParameterInjectionPoint>> entry =
                        injectionPointsMap.entrySet().iterator().next();
                List<ConstructorParameterInjectionPoint> injectionPoints = entry.getValue();
                Object[] arguments = new Object[injectionPoints.size()];
                AnnotatedConstructor annotatedConstructor = entry.getKey();
                Constructor constructor = annotatedConstructor.getJavaMember();
                int i = 0;
                for (ConstructorParameterInjectionPoint injectionPoint : injectionPoints) {
                    if (constructor == null) {
                        constructor = injectionPoint.getMember();
                    }
                    arguments[i++] = beanManager.getInjectableReference(injectionPoint, creationalContext);
                }
                instance = (T) constructor.newInstance(arguments);
            }
            creationalContext.push(instance);
        } catch (Throwable e) {
            throw new CreationException(e);
        }
        return instance;
    }

    @Override
    public void destroy(T instance, CreationalContext<T> creationalContext) {
        // TODO
        creationalContext.release();
    }

    public AnnotatedType getAnnotatedType() {
        return annotatedType;
    }

    public Map<AnnotatedConstructor, List<ConstructorParameterInjectionPoint>> getConstructorParameterInjectionPointsMap() {
        if (constructorParameterInjectionPointsMap == null) {
            constructorParameterInjectionPointsMap = Injections.getConstructorParameterInjectionPointsMap(getAnnotatedType(), this);
        }
        return constructorParameterInjectionPointsMap;
    }

    public List<ConstructorParameterInjectionPoint> getConstructorParameterInjectionPoints() {
        Map<AnnotatedConstructor, List<ConstructorParameterInjectionPoint>> injectionPointsMap =
                getConstructorParameterInjectionPointsMap();
        if (injectionPointsMap.isEmpty()) {
            return emptyList();
        }
        List<ConstructorParameterInjectionPoint> injectionPoints = new LinkedList<>();
        injectionPointsMap.values().forEach(injectionPoints::addAll);
        return unmodifiableList(injectionPoints);
    }

    public Set<FieldInjectionPoint> getFieldInjectionPoints() {
        if (fieldInjectionPoints == null) {
            fieldInjectionPoints = Injections.getFieldInjectionPoints(getAnnotatedType(), this);
        }
        return fieldInjectionPoints;
    }

    public Map<AnnotatedMethod, List<MethodParameterInjectionPoint>> getMethodParameterInjectionPointsMap() {
        if (methodParameterInjectionPointsMap == null) {
            methodParameterInjectionPointsMap = Injections.getMethodParameterInjectionPoints(getAnnotatedType(), this);
        }
        return methodParameterInjectionPointsMap;
    }

    public List<MethodParameterInjectionPoint> getMethodParameterInjectionPoints() {
        List<MethodParameterInjectionPoint> injectionPoints = new LinkedList<>();
        getMethodParameterInjectionPointsMap().values().forEach(injectionPoints::addAll);
        return unmodifiableList(injectionPoints);
    }

    @Override
    public Set<InjectionPoint> getInjectionPoints() {

        List<ConstructorParameterInjectionPoint> constructorParameterInjectionPoints = getConstructorParameterInjectionPoints();

        Set<FieldInjectionPoint> fieldInjectionPoints = getFieldInjectionPoints();

        List<MethodParameterInjectionPoint> methodParameterInjectionPoints = getMethodParameterInjectionPoints();

        int size = constructorParameterInjectionPoints.size() + fieldInjectionPoints.size()
                + methodParameterInjectionPoints.size();

        Set<InjectionPoint> injectionPoints = newLinkedHashSet(size);
        // add the InjectionPoints from Constructors' parameters
        injectionPoints.addAll(constructorParameterInjectionPoints);
        // add the InjectionPoints from Fields
        injectionPoints.addAll(fieldInjectionPoints);
        // add the InjectionPoints from Methods' parameters
        injectionPoints.addAll(methodParameterInjectionPoints);

        return unmodifiableSet(injectionPoints);
    }

    public BeanManager getBeanManager() {
        return beanManager;
    }
}
