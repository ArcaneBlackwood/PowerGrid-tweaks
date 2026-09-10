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

/**
 * @param start Include
 * @param end
 * @param shade
 */
public record Line(boolean vertical, int position, int start, int end, byte shade) {
	public boolean intersects(final Line other) {
		if (this == other) return false;
		if (other.vertical == vertical)
			return other.position == position && other.start <= end && start <= other.end;
		return other.position >= start && other.position <= end
			&& position >= other.start && position <= other.end;
	}
	public boolean touches(final Line other) {
		if (this == other) return false;
		if (other.vertical == vertical) {
			int diff = other.position - position;
			if (diff == -1 || diff == 1) return other.position >= start && other.position <= end
			&& position >= other.start && position <= other.end;
			return diff == 0 && (other.start == end+1 || start == other.end+1);
		}
		if (start == other.position+1 || other.position == end+1) //Is ontop one another
			return position >= other.start && position <= other.end; //Check position falls inside other
		if (other.start == position+1 || position == other.end+1)
			return other.position >= start && other.position <= end;
		return false;
	}
	public Relation getRelation(final Line other) {
		if (intersects(other)) return Relation.INTERSECT;
		if (touches(other)) return Relation.TOUCHING;
		return null;
	}
	public boolean intersects(boolean vertical2, int position2, int start2, int end2) {
		if (vertical2 == vertical)
			return position2 == position && start2 <= end && start <= end2;
		return position2 >= start && position2 <= end
			&& position >= start2 && position <= end2;
	}
	@Override
	public final int hashCode() {
		return (vertical?1:0) | (shade << 1) | (position << 8) | (start << 16) | (start << 24);
	}
	public static enum Relation {
		TOUCHING, INTERSECT;
	}
	public static record Related(Line line, Relation relation) {
		@Override
		public final boolean equals(Object obj) {
			return obj == this
				|| (obj instanceof Related that && that.line == line)
				|| (obj instanceof Line thatLine && thatLine == line);
		}
		@Override
		public final int hashCode() {
			return line == null ? 0 : line.hashCode();
		}
	}
}