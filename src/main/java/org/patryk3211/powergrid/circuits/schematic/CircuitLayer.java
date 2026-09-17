/*
 * Copyright 2025 patryk3211
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.patryk3211.powergrid.circuits.schematic;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Stream;

import org.patryk3211.powergrid.PowerGrid;
import org.patryk3211.powergrid.circuits.schematic.Line.Relations;
import net.minecraft.nbt.LongArrayTag;

public class CircuitLayer {
    public static final int GRID_SIZE = 16;
    public static final int GRID_TO_GRID_SCALE = GRID_SIZE / 16;

    private TraceMatrix traces;

    // Mirror data stored in traces; recalculated when necessary
    private final List<Line> verticalLines;
    private final List<Line> horizontalLines;
    private Relations relations = null;

    public CircuitLayer() {
        traces = new TraceMatrix();
        verticalLines = new ArrayList<>();
        horizontalLines = new ArrayList<>();
    }

    public void from(CircuitLayer other) {
        traces = new TraceMatrix(other.traces);
        updateLines();
    }

    public LongArrayTag serializeNbt() {
        return traces.serializeNbt();
    }

    public void deserialize(long[] tag, boolean fullPixelTraces) {
        traces.deserialize(tag, fullPixelTraces);
        updateLines();
    }

    public boolean hasTrace(int x, int y) {
        return traces.hasTrace(x, y);
    }

    public boolean hasVerticalTrace(int x, int y) {
        return traces.get(x, y, TraceMatrix.TraceDirection.UP) || traces.get(x, y, TraceMatrix.TraceDirection.DOWN);
    }

    public boolean hasHorizontalTrace(int x, int y) {
        return traces.get(x, y, TraceMatrix.TraceDirection.LEFT) || traces.get(x, y, TraceMatrix.TraceDirection.RIGHT);
    }

    public boolean get(int x, int y, TraceMatrix.TraceDirection dir) {
        return traces.get(x, y, dir);
    }

    private void updateLines() {
        verticalLines.clear();
        horizontalLines.clear();

        // Vertical lines
        Integer start = null;
        for (int x = 0; x < GRID_SIZE; x++) {
            for (int y = 0; y < GRID_SIZE; y++) {
                if (start != null && !traces.get(x, y, TraceMatrix.TraceDirection.UP)) {
                    PowerGrid.LOGGER.warn("Illegal state: broken vertical trace; repairing");
                    traces.set(x, y, TraceMatrix.TraceDirection.UP, true);
                }
                if (start == null && traces.get(x, y, TraceMatrix.TraceDirection.DOWN)) {
                    start = y;
                }
                else if (start != null && !traces.get(x, y, TraceMatrix.TraceDirection.DOWN)) {
                    verticalLines.add(new Line(true, x, start, y));
                    start = null;
                }
            }
        }

        // Horizontal lines
        start = null;
        for (int y = 0; y < GRID_SIZE; y++) {
            for (int x = 0; x < GRID_SIZE; x++) {
                if (start != null && !traces.get(x, y, TraceMatrix.TraceDirection.LEFT)) {
                    PowerGrid.LOGGER.warn("Illegal state: broken horizontal trace; repairing");
                    traces.set(x, y, TraceMatrix.TraceDirection.LEFT, true);
                }
                if (start == null && traces.get(x, y, TraceMatrix.TraceDirection.RIGHT)) {
                    start = x;
                }
                else if (start != null && !traces.get(x, y, TraceMatrix.TraceDirection.RIGHT)) {
                    horizontalLines.add(new Line(false, y, start, x));
                    start = null;
                }
            }
        }

        recomputeRelations();
    }

    public void recomputeRelations() {
        int vertSize = verticalLines.size(), totalSize = vertSize + horizontalLines.size();
        if (relations == null) relations = new Relations(totalSize);
        relations.clear();
        for (int i = 0; i < totalSize; i++) {
            Line line = i < vertSize ? verticalLines.get(i) : horizontalLines.get(i-vertSize);
            relations.put(line, new Relations.Entry());
        }
        for (int i = 0; i < totalSize-1; i++) {
            Line line = i < vertSize ? verticalLines.get(i) : horizontalLines.get(i-vertSize);
            Relations.Entry relate = relations.get(line);
            
            for (int j = i+1; j < totalSize; j++) {
                Line test = j < vertSize ? verticalLines.get(j) : horizontalLines.get(j-vertSize);
                Line.Relation result = line.getRelation(test);
                if (result.isNone()) continue;
                relate.put(test, result);
                relations.get(test).put(line, result.contextInvert());
            }
        }
    }

    public List<Line> readVerticalLines() {
        return Collections.unmodifiableList(verticalLines);
    }

    public List<Line> readHorizontalLines() {
        return Collections.unmodifiableList(horizontalLines);
    }

    public Stream<Line> streamLines() {
        return Stream.concat(verticalLines.stream(), horizontalLines.stream());
    }

    public Relations readRelations() {
        if (relations == null) recomputeRelations();
        if (relations.size() != horizontalLines.size() + verticalLines.size())
            throw new IllegalStateException("Calculated relations key count does not match total line count "+relations.size()+" != "+(horizontalLines.size() + verticalLines.size()));
        return relations;
    }
    private void addLine(List<Line> lines, boolean vertical, int position, int start, int end) {
        if (relations == null) recomputeRelations();
        int newStart = start;
        int newEnd = end;
        for (Iterator<Line> iter = lines.iterator(); iter.hasNext();) {
            Line line = iter.next();
            if (line.vertical == vertical && line.position == position &&
                    start <= line.end && line.start <= end) {
                newStart = Math.min(newStart, line.start);
                newEnd = Math.max(newEnd, line.end);
                iter.remove();
                relations.remove(line);
            }
        }
        Line newLine = new Line(vertical, position, newStart, newEnd);
        Relations.Entry relate = new Relations.Entry();
        int vertSize = verticalLines.size(), totalSize = vertSize + horizontalLines.size();
        for (int i = 0; i < totalSize; i++) {
            Line test = i < vertSize ? verticalLines.get(i) : horizontalLines.get(i-vertSize);
            Line.Relation relationTest = test.getRelation(newLine);
            if (relationTest.isNone()) continue;
            relate.put(test, relationTest);
            relations.get(test).put(newLine, relationTest.contextInvert());
        }
        relations.put(newLine, relate);
        lines.add(newLine);
        if (relations.size() != horizontalLines.size() + verticalLines.size())
            throw new IllegalStateException("Calculated relations key count does not match total line count "+relations.size()+" != "+(horizontalLines.size() + verticalLines.size()));
    }

    public void addVerticalLine(int x, int y1, int y2) {
        for (int y = y1; y <= y2; y++) {
            if (y1 < y) {
                traces.set(x, y, TraceMatrix.TraceDirection.UP, true);
            }
            if (y < y2) {
                traces.set(x, y, TraceMatrix.TraceDirection.DOWN, true);
            }
        }

        addLine(verticalLines, true, x, y1, y2);
    }

    public void addHorizontalLine(int y, int x1, int x2) {
        for (int x = x1; x <= x2; x++) {
            if (x1 < x) {
                traces.set(x, y, TraceMatrix.TraceDirection.LEFT, true);
            }
            if (x < x2) {
                traces.set(x, y, TraceMatrix.TraceDirection.RIGHT, true);
            }
        }

        addLine(horizontalLines, false, y, x1, x2);
    }

    public void clear(int x1, int y1, int x2, int y2) {
        traces.clear(x1, y1, x2, y2);
        updateLines();
    }

    public void clear() {
        clear(0, 0, GRID_SIZE - 1, GRID_SIZE - 1);
    }
}
