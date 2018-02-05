/*
 * rumors - a simple discovery/connection protocol
 * Copyright 2011-2018 MeBigFatGuy.com
 * Copyright 2011-2018 Dave Brosius
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations
 * under the License.
 */
package com.mebigfatguy.rumors.impl;

import java.util.List;

import com.mebigfatguy.rumors.Endpoint;

public class EndpointMessage {

    private boolean adding;
    private List<Endpoint> endpoints;

    public EndpointMessage(boolean adding, List<Endpoint> endpoints) {
        this.adding = adding;
        this.endpoints = endpoints;
    }

    public boolean isAdding() {
        return adding;
    }

    public List<Endpoint> getEndpoints() {
        return endpoints;
    }

    @Override
    public int hashCode() {
        int hc = endpoints.hashCode();
        return adding ? hc : -hc;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof EndpointMessage)) {
            return false;
        }

        EndpointMessage that = (EndpointMessage) o;
        return (adding == that.adding) && endpoints.equals(that.endpoints);
    }

    @Override
    public String toString() {
        return "[adding: " + adding + ", endpoints: " + endpoints + "]";
    }
}
