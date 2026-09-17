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
 * limitations under the License.return [$1];
 */
package org.patryk3211.powergrid.circuits.schematic;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;

/**
 * @param start Include
 * @param end
 * @param shade
 */
public class Line {
	public boolean vertical;
	public int position, start, end;
	public byte shade = -1;
	public Clip sizeCut = Clip.NONE;


	public Line() {}
	public Line(boolean vertical, int position, int start, int end) {
		this.vertical = vertical;
		this.position = position;
		this.start = start;
		this.end = end;
	}


	public Relation getRelation(final Line that) {
		if (equals(that)) return Relation.NONE;
		final boolean thisSize1 = end == start;
		final boolean otherSize1 = that.end == that.start;
		if (thisSize1 && otherSize1 && position == that.start && that.position == start)
			return Relation.NONE; //Should be impossible, check anyway

		if (vertical == that.vertical) {
			if (position == that.position) { //Same position
				if (end == that.start)
					return Relation.CLIP_END; //This end clipped by first start
				if (start == that.end)
					return Relation.CLIP_END_OTHER; //Other end clipped by start
				if (start == that.end + 1 || end + 1 == that.start)
					return Relation.TOUCHING; //End or start just next to other
			}
			if (start > that.end || end < that.start)
				return Relation.NONE; //Not overlapping
			if (position == that.position)
				return Relation.INTERSECT; //Overlapping and same position
			if (position + 1 == that.position || position - 1 == that.position)
				return Relation.TOUCHING; //Above one another and overlappig
			return Relation.NONE;
		}

		final boolean thisPosOutside =
			position < that.start ||
			position > that.end;
		final boolean otherPosOutside =
			start > that.position ||
			end < that.position;

		if (thisPosOutside && otherPosOutside) return Relation.NONE;

		if (otherPosOutside)//This pos falls inside of other bounds
			return start - 1 == that.position ||
				end + 1 == that.position
				? Relation.TOUCHING
				: Relation.NONE;
		if (thisPosOutside)//Other pos falls inside of this bounds
			return position + 1 == that.start ||
				position - 1 == that.end
				? Relation.TOUCHING
				: Relation.NONE;

		if (vertical) {//This has priority
			if (start == that.position)
				return Relation.CLIP_START;
			if (end == that.position)
				return Relation.CLIP_END;
		}
		if (that.start == position)
			return Relation.CLIP_START_OTHER;
		if (that.end == position)
			return Relation.CLIP_END_OTHER;
		if (start == that.position)
			return Relation.CLIP_START;
		if (end == that.position)
			return Relation.CLIP_END;

		return Relation.INTERSECT;
	}


	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Line[vert=");
		sb.append(vertical).append(", pos=").append(position).append(", start=").append(start)
		.append(", end=").append(end).append(", shade=").append(shade).append(", cut=").append(sizeCut);
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



	public static enum Clip {
		NONE(0,0), START(1,0), BOTH(1,-1), END(0,-1);
		private byte start, end;
		private Clip(int start, int end) {
			this.start = (byte)start;
			this.end = (byte)end;
		}
		public int getStart(Line line) {
			return line.start + this.start;
		}
		public int getEnd(Line line) {
			return line.end + this.end;
		}
		public Clip combine(Clip other) {
			if (other == null || other == NONE) return this;
			if (this == NONE || this == other) return other;
			return BOTH; 
		}
	}
	public static enum Relation {
		NONE(false),
		TOUCHING(false),
		INTERSECT(true),
		/**
		 * This line start intersect with argument line.  Vertical or first(when both parallel) have clip prioroty
		 */
		CLIP_START(true, Clip.START),
		/**
		 * This line end intersect with argument line.  Vertical or first(when both parallel) have clip prioroty
		 */
		CLIP_END(true, Clip.END),
		CLIP_START_OTHER(true),
		CLIP_END_OTHER(true);

		private final boolean isIntersect;
		private final Clip clip;
		private Relation(boolean isIntersect, Clip clip) {
			this.isIntersect = isIntersect;
			this.clip = clip;
		}
		private Relation(boolean isIntersect) {
			this.isIntersect = isIntersect;
			this.clip = Clip.NONE;
		}

		public boolean isIntersect() {
			return isIntersect;
		}
		public boolean isNone() {
			return this == NONE;
		}
		public boolean isTouch() {
			return this == TOUCHING;
		}
		public Clip getClip() {
			return clip;
		}
		public Relation contextInvert() {
			return switch(this) {
				case CLIP_START -> CLIP_START_OTHER;
				case CLIP_END -> CLIP_END_OTHER;
				case CLIP_START_OTHER -> CLIP_START;
				case CLIP_END_OTHER -> CLIP_END;
				default -> this;
			};
		}
	}
	public static class Relations extends Reference2ObjectOpenHashMap<Line, Relations.Entry> {
		public Relations(int initialSize) {
			super(initialSize);
		}
		public static class Entry extends Reference2ObjectOpenHashMap<Line, Relation> {}
	}
}