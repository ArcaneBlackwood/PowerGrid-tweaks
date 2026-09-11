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
public class Line {
	public boolean vertical;
	public int position, start, end;
	public byte shade = -1;


	public Line() {}
	public Line(boolean vertical, int position, int start, int end) {
		this.vertical = vertical;
		this.position = position;
		this.start = start;
		this.end = end;
	}


	public Relation getRelation(final Line other) { //
		if (this.equals(other)) return null;
		if (vertical == other.vertical) { //Parallel
			if (start > other.end && end < other.start) return null; //Doesnt overlap
			if (position+1 == other.position || position-1 == other.position) //Above one another
				return Relation.TOUCHING;

			if (position != other.position) return null; //Not aligned
			if (end == other.start) return Relation.CLIP_START;
			if (start == other.end+1 || end+1 == other.start) return Relation.TOUCHING;
			return Relation.INTERSECT;
		} //else Perpindicular
		
		boolean thisPosOutside = position < other.start || position > other.end;
		boolean otherPosOutside = start > other.position || end < other.position;
		if (thisPosOutside && otherPosOutside) return null; //Cant intersect at all

		if (otherPosOutside) //This pos inside, check if other pos right next to
			return start-1 == other.position || end+1 == other.position ? //Ends touching other
				Relation.TOUCHING : null;
		if (thisPosOutside) //Other pos inside, check if this pos right next to
			return position-1 == other.end || position+1 == other.end ? //Touching other ends
				Relation.TOUCHING : null;
		//else Must intersect somewhere(no pos outside)

		if (vertical && start == other.position) return Relation.CLIP_START;
		if (vertical && end == other.position) return Relation.CLIP_END;
		return Relation.INTERSECT; //Doesnt have priority or start/end not clipped by other
	}
	public boolean intersects(boolean vertical2, int position2, int start2, int end2) {
		if (vertical2 == vertical)
			return position2 == position && start2 <= end && start <= end2;
		return position2 >= start && position2 <= end
			&& position >= start2 && position <= end2;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Line[vert=");
		sb.append(vertical).append(", pos=").append(position).append(", start=")
			.append(start).append(", end=").append(end).append(", shade=").append(shade);
		sb.append("]#").append(hashCode());
		return sb.toString();
	}
	@Override
	public final boolean equals(Object obj) {
		if (!(obj instanceof Line other)) return false;
		return other == this ||
			(vertical == other.vertical && position == other.position && start == other.start
			&& end == other.end);
	}



	public static enum Relation {
		TOUCHING, INTERSECT,
		/**
		 * This line start intersect with argument line.  Vertical or first(when both parallel) have clip prioroty
		 */
		CLIP_START,
		/**
		 * This line end intersect with argument line.  Vertical or first(when both parallel) have clip prioroty
		 */
		CLIP_END;
		public boolean isIntersect() {
			return this != TOUCHING;
		}
	}
	public static record Related(Line line, Relation relation) { }
}