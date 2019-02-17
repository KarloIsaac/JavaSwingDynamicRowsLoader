package mx.gob.cofepris.cos.deds.dynamicRowsLoader;

import java.awt.Component;

/**
 * Must return the component that shall be displayed in a given position.
 * 
 * @author kijimenez
 *
 */
public interface ToDisplayComponentRenderer {

	/**
	 * Returns the Component that must be displayed at the given index. It is convenient that
	 * the Component's construction process does not take too much time; otherwise, the loading
	 * of the rows will be slow.
	 * 
	 * @param componentIndex the index of the component that will be built. This index is 
	 * based on how many row-heights were registered through the {@link 
	 * MultiRowViewRenditionManager#setPositionHeight setPositionHeight}. The index starts
	 * at 0 and the last componet's index would be the number of times the aforementioned
	 * method was called minus one.
	 * 
	 * @return the component to display on the currently visible portion of the 
	 * {@link MultiRowViewRenditionManager#getComponentsDisplayScroll scroll pane}
	 */
	public Component retrieveComponent(int componentIndex);
}
