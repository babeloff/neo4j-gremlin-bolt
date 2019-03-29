/*
 *  Copyright 2016 SteelBridge Laboratories, LLC.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *  For more information: http://steelbridgelabs.com
 */

package com.steelbridgelabs.oss.neo4j.structure;

import org.apache.tinkerpop.gremlin.structure.Graph;
import org.apache.tinkerpop.gremlin.structure.Transaction;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.neo4j.driver.v1.Values;
import org.neo4j.driver.v1.types.Node;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Rogelio J. Baucells
 */
@RunWith(MockitoJUnitRunner.class)
public class Neo4JVertexWhileAddingPropertyValueTest {
    @Rule
    public ErrorCollector collector = new ErrorCollector();

    @Mock
    private Neo4JGraph graph;

    @Mock
    private Transaction transaction;

    @Mock
    private Neo4JSession session;

    @Mock
    private Neo4JReadPartition partition;

    @Mock
    private Node node;

    @Mock
    private Neo4JElementIdProvider provider;

    @Mock
    private Graph.Features.VertexFeatures vertexFeatures;

    @Mock
    private Graph.Features features;

    @Test
    public void givenPropertyWithSingleCardinalityShouldAddItToVertex() {
        // arrange
        Mockito.when(vertexFeatures.getCardinality(Mockito.anyString())).thenAnswer(invocation -> VertexProperty.Cardinality.single);
        Mockito.when(features.vertex()).thenAnswer(invocation -> vertexFeatures);
        Mockito.when(partition.validateLabel(Mockito.anyString())).thenAnswer(invocation -> true);
        Mockito.when(graph.tx()).thenAnswer(invocation -> transaction);
        Mockito.when(graph.getPartition()).thenAnswer(invocation -> partition);
        Mockito.when(graph.features()).thenAnswer(invocation -> features);
        Mockito.when(node.get(Mockito.eq("id"))).thenAnswer(invocation -> Values.value(1L));
        Mockito.when(node.labels()).thenAnswer(invocation -> Collections.singletonList("l1"));
        Mockito.when(node.keys()).thenAnswer(invocation -> Collections.emptyList());
        Mockito.when(provider.generate()).thenAnswer(invocation -> 2L);
        Mockito.when(provider.fieldName()).thenAnswer(invocation -> "id");


        Neo4JVertex vertex = new Neo4JVertex(graph, session, provider, provider, node);
        // act
        HashMap<Neo4JPropertySamples, VertexProperty<?>> results = new HashMap<>(Neo4JPropertySamples.values().length);
        for ( Neo4JPropertySamples sam : Neo4JPropertySamples.values() ) {
            try {
                results.put(sam, vertex.property(VertexProperty.Cardinality.single, sam.title, sam.value));
            } catch (IllegalArgumentException ex) {
                if (! sam.supported) continue;

                StringBuffer sb = new StringBuffer("could not create vertex property ")
                        .append(" [").append(sam).append("] ")
                        .append('\n').append("Stacktrace:").append('\n');
                for (StackTraceElement element : ex.getStackTrace() ) {
                    sb.append(element.toString());
                };
                collector.addError(new Throwable(sb.toString()));
            }
        }

        // assert
        for ( Map.Entry<Neo4JPropertySamples,VertexProperty<?>> entry : results.entrySet() ) {
            Neo4JPropertySamples key = entry.getKey();
            VertexProperty<?> result = entry.getValue();
            Assert.assertNotNull("Failed to add property to vertex", result);
            Assert.assertTrue("Property value is not present", result.isPresent());
            Assert.assertTrue("Failed to set vertex as dirty", vertex.isDirty());
            Assert.assertEquals("Invalid property key", result.key(), key.title);
            Assert.assertEquals("Invalid property value", result.value(), key.value);
            Assert.assertEquals("Invalid property element", result.element(), vertex);
        }
    }

    @Test
    public void givenPropertyWithListCardinalityShouldAddItToVertex() {
        // arrange
        Mockito.when(vertexFeatures.getCardinality(Mockito.anyString())).thenAnswer(invocation -> VertexProperty.Cardinality.single);
        Mockito.when(features.vertex()).thenAnswer(invocation -> vertexFeatures);
        Mockito.when(partition.validateLabel(Mockito.anyString())).thenAnswer(invocation -> true);
        Mockito.when(graph.tx()).thenAnswer(invocation -> transaction);
        Mockito.when(graph.getPartition()).thenAnswer(invocation -> partition);
        Mockito.when(graph.features()).thenAnswer(invocation -> features);
        Mockito.when(node.get(Mockito.eq("id"))).thenAnswer(invocation -> Values.value(1L));
        Mockito.when(node.labels()).thenAnswer(invocation -> Collections.singletonList("l1"));
        Mockito.when(node.keys()).thenAnswer(invocation -> Collections.emptyList());
        Mockito.when(provider.generate()).thenAnswer(invocation -> 2L);
        Mockito.when(provider.fieldName()).thenAnswer(invocation -> "id");
        Neo4JVertex vertex = new Neo4JVertex(graph, session, provider, provider, node);
        // act
        Neo4JPropertySamples sample = Neo4JPropertySamples.LONG;
        VertexProperty<?> result = vertex.property(VertexProperty.Cardinality.list, sample.title, sample.value);
        // assert
        Assert.assertNotNull("Failed to add property to vertex", result);
        Assert.assertTrue("Property value is not present", result.isPresent());
        Assert.assertTrue("Failed to set vertex as dirty", vertex.isDirty());
        Assert.assertEquals("Invalid property key", result.key(), sample.title);
        Assert.assertEquals("Invalid property value", result.value(), sample.value);
        Assert.assertEquals("Invalid property element", result.element(), vertex);
        // act
        result = vertex.property(VertexProperty.Cardinality.list, sample.title, sample.value);
        // assert
        Assert.assertNotNull("Failed to add property to vertex", result);
        Assert.assertTrue("Property value is not present", result.isPresent());
        Assert.assertTrue("Failed to set vertex as dirty", vertex.isDirty());
        Assert.assertEquals("Invalid property key", result.key(), sample.title);
        Assert.assertEquals("Invalid property value", result.value(), sample.value);
        Assert.assertEquals("Invalid property element", result.element(), vertex);
        Iterator<Long> values = vertex.values(sample.title);
        Assert.assertTrue("Invalid number of property values", values.hasNext());
        Assert.assertEquals("Invalid property value", values.next(), (Long)sample.value);
        Assert.assertTrue("Invalid number of property values", values.hasNext());
        Assert.assertEquals("Invalid property value", values.next(), (Long)sample.value);
        Assert.assertFalse("Invalid number of property values", values.hasNext());
    }

    @Test
    public void givenPropertyWithSetCardinalityShouldAddItToVertex() {
        // arrange
        Mockito.when(vertexFeatures.getCardinality(Mockito.anyString())).thenAnswer(invocation -> VertexProperty.Cardinality.single);
        Mockito.when(features.vertex()).thenAnswer(invocation -> vertexFeatures);
        Mockito.when(partition.validateLabel(Mockito.anyString())).thenAnswer(invocation -> true);
        Mockito.when(graph.tx()).thenAnswer(invocation -> transaction);
        Mockito.when(graph.getPartition()).thenAnswer(invocation -> partition);
        Mockito.when(graph.features()).thenAnswer(invocation -> features);
        Mockito.when(node.get(Mockito.eq("id"))).thenAnswer(invocation -> Values.value(1L));
        Mockito.when(node.labels()).thenAnswer(invocation -> Collections.singletonList("l1"));
        Mockito.when(node.keys()).thenAnswer(invocation -> Collections.emptyList());
        Mockito.when(provider.generate()).thenAnswer(invocation -> 2L);
        Mockito.when(provider.fieldName()).thenAnswer(invocation -> "id");
        Neo4JVertex vertex = new Neo4JVertex(graph, session, provider, provider, node);
        // act
        Neo4JPropertySamples sample = Neo4JPropertySamples.LONG;
        VertexProperty<?> result = vertex.property(VertexProperty.Cardinality.set, sample.title, sample.value);
        // assert
        Assert.assertNotNull("Failed to add property to vertex", result);
        Assert.assertTrue("Property value is not present", result.isPresent());
        Assert.assertTrue("Failed to set vertex as dirty", vertex.isDirty());
        Assert.assertEquals("Invalid property value", result.value(), sample.value);
        Assert.assertEquals("Invalid property element", result.element(), vertex);
        // act
        result = vertex.property(VertexProperty.Cardinality.set, sample.title, sample.value);
        // assert
        Assert.assertNotNull("Failed to add property to vertex", result);
        Assert.assertTrue("Property value is not present", result.isPresent());
        Assert.assertTrue("Failed to set vertex as dirty", vertex.isDirty());
        Assert.assertEquals("Invalid property value", result.value(), sample.value);
        Iterator<Long> values = vertex.values(sample.title);
        Assert.assertTrue("Invalid number of property values", values.hasNext());
        Assert.assertEquals("Invalid property value", values.next(), (Long)sample.value);
        Assert.assertFalse("Invalid number of property values", values.hasNext());
    }

    @Test(expected = IllegalArgumentException.class)
    public void givenPropertyWithCardinalityShouldKeepTheSameCardinality() {
        // arrange
        Mockito.when(vertexFeatures.getCardinality(Mockito.anyString())).thenAnswer(invocation -> VertexProperty.Cardinality.single);
        Mockito.when(features.vertex()).thenAnswer(invocation -> vertexFeatures);
        Mockito.when(partition.validateLabel(Mockito.anyString())).thenAnswer(invocation -> true);
        Mockito.when(graph.tx()).thenAnswer(invocation -> transaction);
        Mockito.when(graph.getPartition()).thenAnswer(invocation -> partition);
        Mockito.when(graph.features()).thenAnswer(invocation -> features);
        Mockito.when(node.get(Mockito.eq("id"))).thenAnswer(invocation -> Values.value(1L));
        Mockito.when(node.labels()).thenAnswer(invocation -> Collections.singletonList("l1"));
        Mockito.when(node.keys()).thenAnswer(invocation -> Collections.emptyList());
        Mockito.when(provider.generate()).thenAnswer(invocation -> 2L);
        Mockito.when(provider.fieldName()).thenAnswer(invocation -> "id");
        Neo4JVertex vertex = new Neo4JVertex(graph, session, provider, provider, node);

        Neo4JPropertySamples sample = Neo4JPropertySamples.LONG;
        vertex.property(VertexProperty.Cardinality.single, sample.title, sample.value);
        // act
        vertex.property(VertexProperty.Cardinality.list, sample.title, sample.value);
        // assert
        Assert.fail("Failed to detect property with different cardinality");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void givenPropertyWithMetaPropertyShouldThrowException() {
        // arrange
        Mockito.when(vertexFeatures.getCardinality(Mockito.anyString())).thenAnswer(invocation -> VertexProperty.Cardinality.single);
        Mockito.when(features.vertex()).thenAnswer(invocation -> vertexFeatures);
        Mockito.when(partition.validateLabel(Mockito.anyString())).thenAnswer(invocation -> true);
        Mockito.when(graph.tx()).thenAnswer(invocation -> transaction);
        Mockito.when(graph.getPartition()).thenAnswer(invocation -> partition);
        Mockito.when(graph.features()).thenAnswer(invocation -> features);
        Mockito.when(node.get(Mockito.eq("id"))).thenAnswer(invocation -> Values.value(1L));
        Mockito.when(node.labels()).thenAnswer(invocation -> Collections.singletonList("l1"));
        Mockito.when(node.keys()).thenAnswer(invocation -> Collections.emptyList());
        Mockito.when(provider.generate()).thenAnswer(invocation -> 2L);
        Mockito.when(provider.fieldName()).thenAnswer(invocation -> "id");
        Neo4JVertex vertex = new Neo4JVertex(graph, session, provider, provider, node);
        // act
        Neo4JPropertySamples sample = Neo4JPropertySamples.LONG;
        vertex.property(VertexProperty.Cardinality.single, sample.title, sample.value, "a", 2L);
        // assert
        Assert.fail("Failed to prevent property with meta properties");
    }
}
