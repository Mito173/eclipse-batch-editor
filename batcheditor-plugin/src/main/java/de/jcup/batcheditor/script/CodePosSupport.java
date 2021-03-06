package de.jcup.batcheditor.script;

public interface CodePosSupport {

	/**
	 * Moves to new position
	 * @param newPos
	 */
	void moveToPos(int newPos);

	/**
	 * Get initial start position inside code fragment
	 * @return start position
	 */
	int getInitialStartPos();

	/**
	 * Gives back character at wanted position, or <code>null</code>
	 * @param pos
	 * @return character at wanted position, or <code>null</code>
	 */
	Character getCharacterAtPosOrNull(int pos);

}
