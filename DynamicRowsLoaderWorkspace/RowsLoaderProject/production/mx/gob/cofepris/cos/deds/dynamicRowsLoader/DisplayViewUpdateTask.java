package mx.gob.cofepris.cos.deds.dynamicRowsLoader;

import java.awt.*;
import java.lang.reflect.*;
import java.util.*;
import javax.swing.*;


class DisplayViewUpdateTask {
	ToDisplayComponentRenderer toDisplayComponentRenderer;
	Map<Integer, Component> displayedComponentsCache;
	ResizableViewPortScroll displayerScroll;
	ArrayList<Integer> heightsList;
	ArrayList<Point> pointsList;
	private int currentInitialVisibleIndex = -1;
	private int initialCachedVisibleIndex = -1;
	private int currentFinalVisibleIndex = -1;
	private int finalCachedVisibleIndex = -1;	
	boolean screenWentDown;
	private DisplayUpdateTaskOverListener displayUpdateTaskOverListener;	
	
	
	DisplayViewUpdateTask() {}


	public void displayComponentsInViewRange() {
		setWorkingIndexes();
		displayComponents(screenWentDown);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				displayerScroll.revalidate();
				if (displayUpdateTaskOverListener != null) {
					displayUpdateTaskOverListener.performUpdateTaskIsOverActions();
				}
			}			
		});
	}


	private void setWorkingIndexes() {		
		Point firstPoint = retrieveVisibleViewEdgePoint(false);
		Point lastPoint = retrieveVisibleViewEdgePoint(true);
		currentInitialVisibleIndex = pointsList.indexOf(firstPoint);
		currentFinalVisibleIndex = pointsList.indexOf(lastPoint);	
		if (!displayedComponentsCache.isEmpty()) {
			TreeSet<Integer> orderedCachedIndexesSet = new TreeSet<Integer>();
			orderedCachedIndexesSet.addAll(displayedComponentsCache.keySet());
			setInitialCachedVisibleIndex(orderedCachedIndexesSet);
			setFinalCachedVisibleIndex(orderedCachedIndexesSet);
		}
	}


	private Point retrieveVisibleViewEdgePoint(boolean isLastPoint) {
		Dimension visibleSize = displayerScroll.getViewport().getExtentSize();
		int verticalSpace = isLastPoint ? visibleSize.height : 0;
		Point position = displayerScroll.getViewport().getViewPosition();
		position = new Point(0, (position.y + verticalSpace));
		while (!pointsList.contains(position) && position.y > 0) {
			position = new Point(0, position.y - 1);
		}
		return position;
	}


	private void setInitialCachedVisibleIndex(TreeSet<Integer> orderedCachedIndexesSet) {
		for (int browsedIndex : orderedCachedIndexesSet) {
			if (browsedIndex >= currentInitialVisibleIndex) {
				initialCachedVisibleIndex = browsedIndex;
				break;
			}
		}		
	}


	private void setFinalCachedVisibleIndex(TreeSet<Integer> orderedCachedIndexesSet) {		
		for (int browsedIndex = orderedCachedIndexesSet.last() ; browsedIndex >= 0 ; browsedIndex--) {
			if (browsedIndex <= currentFinalVisibleIndex) {
				finalCachedVisibleIndex = browsedIndex;
				break;
			}
		}
	}


	private void displayComponents(boolean screenWentDown) {
		if (currentInitialVisibleIndex >= 0 && currentFinalVisibleIndex >= 0) {			
			addCachedComponentsInRange();
			if (screenWentDown) {
				iterateFromUpToDown();
			} else {
				iterateFromDownToUp();
			}
			clearNotDisplayedObjectsFromMap();			
		}
	}


	private void addCachedComponentsInRange() {
		try {
			Runnable displayerScrollUpdateThread = createDisplayerScrollUpdateRunnable();			
			SwingUtilities.invokeAndWait(displayerScrollUpdateThread);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}		
	}
	
	
	private Runnable createDisplayerScrollUpdateRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				displayerScroll.clearComponentsViewDisplay();
				if (initialCachedVisibleIndex != -1 && finalCachedVisibleIndex != -1) {
					for (int i = initialCachedVisibleIndex ; i <= finalCachedVisibleIndex ; i++) {
						Component cachedComponent = displayedComponentsCache.get(i);	
						displayerScroll.addComponentToViewDisplay(cachedComponent, true); 
					}
    				displayerScroll.setInicialAndFinalSpaceAdjustHeight(calculateInicialSpaceAdjust(initialCachedVisibleIndex), 
    						calculateFinalSpaceAdjust(finalCachedVisibleIndex));					
				}
			}
		};
	}


	private void iterateFromUpToDown() {
		for (int i = currentInitialVisibleIndex; i <= currentFinalVisibleIndex; i++) {
			if (i < initialCachedVisibleIndex || i > finalCachedVisibleIndex) {
    			new ScrollViewUpdater().setTargetDisplayerScroll(displayerScroll)
    				.setTargetComponent(retrieveMappedComponent(i))
    				.setInicialSpaceHeight(calculateInicialSpaceAdjust(currentInitialVisibleIndex))
    				.setFinalSpaceHeight(calculateFinalSpaceAdjust(i))
    				.setScreenWentDown(true)
    				.updateScrollView();
			}
		}
	}


	private void iterateFromDownToUp() {
		for (int i = currentFinalVisibleIndex; i >= currentInitialVisibleIndex; i--) {	
			if (i < initialCachedVisibleIndex || i > finalCachedVisibleIndex) {
    			new ScrollViewUpdater().setTargetDisplayerScroll(displayerScroll)
    				.setTargetComponent(retrieveMappedComponent(i))
    				.setInicialSpaceHeight(calculateInicialSpaceAdjust(i))
    				.setFinalSpaceHeight(calculateFinalSpaceAdjust(currentFinalVisibleIndex))
    				.setScreenWentDown(false)
    				.updateScrollView();
			}
		}
	}


	private Component retrieveMappedComponent(int componentIndex) {
		Component targetComponent = null;
		if (!displayedComponentsCache.containsKey(componentIndex)) {
			targetComponent = toDisplayComponentRenderer.retrieveComponent(componentIndex);
			displayedComponentsCache.put(componentIndex, targetComponent);
		}
		targetComponent = displayedComponentsCache.get(componentIndex);
		return targetComponent;
	}


	private int calculateInicialSpaceAdjust(int initialPointIndex) {
		Point firstVisiblePoint = pointsList.get(initialPointIndex);
		int inicialSpace = firstVisiblePoint.y;
		return inicialSpace;
	}


	private int calculateFinalSpaceAdjust(int lastPointIndex) {
		Point lastVisiblePoint = pointsList.get(lastPointIndex);
		int lastVisibleHeight = heightsList.get(lastPointIndex);
		int lastVisibleComponentExtent = lastVisiblePoint.y + lastVisibleHeight;
		int lastIndex = heightsList.size() - 1;
		Point lastPoint = pointsList.get(lastIndex);
		int lastHeight = heightsList.get(lastIndex);
		int displayHeight = lastPoint.y + lastHeight;
		int finalSpace = displayHeight - lastVisibleComponentExtent;
		return finalSpace;
	}


	private void clearNotDisplayedObjectsFromMap() {
		Iterator<Integer> ketIterator = displayedComponentsCache.keySet().iterator();
		while (ketIterator.hasNext()) {
			int index = ketIterator.next();
			if (index < currentInitialVisibleIndex || index > currentFinalVisibleIndex) {
				ketIterator.remove();
			}
		}
	}


	public void setDisplayUpdateTaskOverListener(DisplayUpdateTaskOverListener displayUpdateTaskOverListener) {
		this.displayUpdateTaskOverListener = displayUpdateTaskOverListener;		
	}
}
