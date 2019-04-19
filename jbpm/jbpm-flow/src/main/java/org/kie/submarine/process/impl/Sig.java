/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.submarine.process.impl;

import org.kie.submarine.process.Signal;

public final class Sig<T> implements Signal<T> {

    private final String channel;
    private final T payload;

    public static <T> org.kie.submarine.process.Signal<T> of(String channel, T payload) {
        return new Sig<>(channel, payload);
    }

    protected Sig(String channel, T payload) {
        this.channel = channel;
        this.payload = payload;
    }

    @Override
    public String channel() {
        return channel;
    }

    public T payload() {
        return payload;
    }
}
