package mx.gob.cofepris.cos.deds.dynamicRowsLoader;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.function.Consumer;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;


public class MultiRowViewRenditionManager {
	private ToDisplayComponentRenderer toDisplayComponentRenderer;	
	private ArrayList<Point> pointsList = new ArrayList<>();
	private ArrayList<Integer> heightsList = new ArrayList<Integer>();
	private ResizableViewPortScroll displayerScroll = new ResizableViewPortScroll();
	private DisplayViewUpdateTaskScheduler displayViewUpdateTaskScheduler = new DisplayViewUpdateTaskScheduler();
	private int lastScreenPosition = 0;
	private Rectangle visibleScreenRectangle;


	public MultiRowViewRenditionManager(ToDisplayComponentRenderer toDisplayComponentRenderer) {		
		if (toDisplayComponentRenderer == null) {
			throw new IllegalArgumentException();
		}	
		this.toDisplayComponentRenderer = toDisplayComponentRenderer;
		displayerScroll.getVerticalScrollBar().addAdjustmentListener(adjustmentEvent -> {
			if (!adjustmentEvent.getValueIsAdjusting()) {
    			updateScreenOnVisibleDisplayChange(didScreenWentDown());
    		}
		});
	}

	
	private boolean didScreenWentDown() {		
		int currentScreenPosition = displayerScroll.getViewport().getViewPosition().y;
		int positionChange = currentScreenPosition - lastScreenPosition;
		boolean screenWentDown = positionChange >= 0;
		lastScreenPosition = currentScreenPosition;
		return screenWentDown;
	}


	public void setPositionHeight(int targetIndex, int height) {		
		int lastSetIndex = heightsList.size() - 1;
		if (targetIndex <= lastSetIndex && lastSetIndex >= 0) {
			heightsList.set(targetIndex, height);
			int nextIndexToUpdate = targetIndex + 1;
			updatePointsPositions(nextIndexToUpdate);
			scheduleUpdateVisibleComponentsTask(true);
		} else {
			saveNewPointHeightReference(targetIndex, height);			
		}
	}


	private void updatePointsPositions(int nextIndexToUpdate) {		
		for (int indexToUpdate = nextIndexToUpdate ; indexToUpdate < pointsList.size() ; indexToUpdate++) {
			int referenceIndex = indexToUpdate - 1;
			int newYPosition = calculateYPositionFromFormerPoint(referenceIndex);				
			Point newUpdatedPoint = new Point(0, newYPosition);			
			pointsList.set(indexToUpdate, newUpdatedPoint);			
		}
	}
	
	
	private void saveNewPointHeightReference(int targetIndex, int height) {
		int formerIndex = targetIndex - 1;
		int yPosition = formerIndex < 0
				? 0
				: calculateYPositionFromFormerPoint(formerIndex);
		Point newPoint = new Point(0, yPosition);	
		heightsList.add(targetIndex, height);
		pointsList.add(targetIndex, newPoint);
	}


	private int calculateYPositionFromFormerPoint(int formerIndex) {
		Point formerPoint = pointsList.get(formerIndex);
		int formerYPosition = formerPoint.y;
		int formerHeight = heightsList.get(formerIndex);
		int yPosition = formerYPosition + formerHeight;	
		return yPosition;
	}	


	private void updateScreenOnVisibleDisplayChange(boolean screenWentDown) {		
		if (visibleDisplayRequiresUpdate()) {
			scheduleUpdateVisibleComponentsTask(screenWentDown);
		}
	}


	private boolean visibleDisplayRequiresUpdate() {
		boolean updateRequired = false;
		Rectangle currentVisibleRectangle = displayerScroll.getViewport().getViewRect();		
		if (visibleScreenRectangle == null) {
			visibleScreenRectangle = currentVisibleRectangle;			
		} else {			
			Rectangle intersectionRectangle = visibleScreenRectangle.intersection(currentVisibleRectangle);
			intersectionRectangle.setSize(visibleScreenRectangle.width, intersectionRectangle.height);
			Rectangle widthAdjustedComparissionRectangle = currentVisibleRectangle.getBounds();
			widthAdjustedComparissionRectangle.setSize(visibleScreenRectangle.width, widthAdjustedComparissionRectangle.height);
			if (!widthAdjustedComparissionRectangle.equals(intersectionRectangle)) {
				updateRequired = true;
				visibleScreenRectangle = currentVisibleRectangle;	
			}
		}		
		return updateRequired;
	}
	

	public void scheduleUpdateVisibleComponentsTask(boolean screenWentDown) {
		displayViewUpdateTaskScheduler.getPreparedDisplayViewUpdateTaskBuilder()
				.setToDisplayComponentRenderer(toDisplayComponentRenderer)
        		.setDisplayerScroll(displayerScroll)
        		.setHeightsList(heightsList)
        		.setPointsList(pointsList)
        		.setScreenWentDown(screenWentDown)
				.callDisplayViewUpdateTask();
	}
	
	
	public void clearState() {
		displayViewUpdateTaskScheduler.clearState();
		heightsList.clear();
		pointsList.clear();		
		clearComponentsViewDisplay();
	}
	
	
	private void clearComponentsViewDisplay() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				displayerScroll.resetScrollSize();
			}
		});
	}


	public JScrollPane getComponentsDisplayScroll() {		
		return displayerScroll;
	}
	
	
	public void moveViewPortToComonentAtIndex(int componentIndex) {
		Point location = pointsList.get(componentIndex);
		displayerScroll.getVerticalScrollBar().setValue(location.y);
	}
	
	
	public void visitVisibleComponents(Consumer<Component> componentProcessor) {
		Container container = (Container) displayerScroll.getViewport().getView();
		Component[] visibleComponents = container.getComponents();
		for (Component component : visibleComponents) {
			componentProcessor.accept(component);
		}
	}
}