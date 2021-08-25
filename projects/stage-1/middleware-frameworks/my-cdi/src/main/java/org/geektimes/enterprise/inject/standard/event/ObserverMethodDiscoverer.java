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
package org.geektimes.enterprise.inject.standard.event;

import javax.enterprise.inject.spi.ObserverMethod;
import java.util.List;

/**
 * The discoverer for {@link ObserverMethod}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public interface ObserverMethodDiscoverer {

    /**
     * Get the all {@link ObserverMethod observer methods} from the specified bean type
     *
     * @param beanInstance the beanInstance that {@link ObserverMethod observer methods} are belongs to
     *                     if beanInstance is <code>null</code>, the {@link ObserverMethod observer methods}
     *                     should be static
     * @param beanType     the specified bean type
     * @return non-null read-only {@link List}
     */
    <T> List<ObserverMethod<T>> getObserverMethods(T beanInstance, Class<? extends T> beanType);
}
