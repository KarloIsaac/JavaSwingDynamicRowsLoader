package mx.gob.cofepris.cos.deds.dynamicRowsLoader;

import java.awt.Component;
import java.awt.Container;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.function.Consumer;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;


/**
 * Represents the entrance point to the Dynamic Rows Loader API. It wires and prepares 
 * all the necessary components. The end user only needs to operate over this object. 
 * <br>
 * This component provides a prepared {@link JScrollPane}, ready to display rows as the scroll 
 * is moved up or down. The invisible components are removed while those who need to be displayed
 * on the visible screen are added. 
 * <br>
 * Additionally, this object allows other components to register to scrolling events, and 
 * provides methods to adjust the state of the displayed components
 * <br>
 * It is very important that the method {@link #setPositionHeight} is used to set the height that
 * each component would take when displayed on the screen. This is due to the fact that this API
 * requires to know up front this datum to allocate enough space on the screen to position the row
 * components when it should be needed.
 * <br>
 * The aforementioned method will typically be used in two moments. On the one hand, when a new 
 * group of GUI rows will be rendered and it is necessary to allocate space on the screen to 
 * display them. On the other hand, when any of the GUI rows changes its height; this is needed
 * to reposition all the rows appropriately.
 * 
 * @author kijimenez
 *
 */
public class MultiRowViewRenditionManager {
	private ToDisplayComponentRenderer toDisplayComponentRenderer;	
	private ArrayList<Point> pointsList = new ArrayList<>();
	private ArrayList<Integer> heightsList = new ArrayList<Integer>();
	private ResizableViewPortScroll resizableViewPortScroll = new ResizableViewPortScroll();
	private DisplayViewUpdateTaskScheduler displayViewUpdateTaskScheduler = 
			new DisplayViewUpdateTaskScheduler(resizableViewPortScroll);
	private int lastScreenPosition = 0;	
	private Rectangle visibleScreenRectangle;


	/**
	 * Initializes the Dynamic Rows Loader API.
	 * 
	 * @param toDisplayComponentRenderer the component to return the concrete graphic elements 
	 * that will be displayed at each position. 		
	 * 
	 * @throws IllegalArgumentException If the ToDisplayComponentRenderer argument is not 
	 * indicated 
	 */
	public MultiRowViewRenditionManager(ToDisplayComponentRenderer toDisplayComponentRenderer) {		
		if (toDisplayComponentRenderer == null) {
			throw new IllegalArgumentException();
		}	
		this.toDisplayComponentRenderer = toDisplayComponentRenderer;
		resizableViewPortScroll.getVerticalScrollBar().addAdjustmentListener(adjustmentEvent -> {
			if (!adjustmentEvent.getValueIsAdjusting()) {
    			updateScreenOnVisibleDisplayChange(didScreenWentDown());
    		}
		});
	}

	
	private boolean didScreenWentDown() {		
		int currentScreenPosition = resizableViewPortScroll.getViewport().getViewPosition().y;
		int positionChange = currentScreenPosition - lastScreenPosition;
		boolean screenWentDown = positionChange >= 0;
		lastScreenPosition = currentScreenPosition;
		return screenWentDown;
	}


	/**
	 * Sets the height that the i<sup>th</sup> GUI row would need in order to be completely 
	 * displayed on the screen. In case the height of a previously registered row is being
	 * updated, this method will update the positions reserved for each GUI row and will schedule
	 * the update of the {@link JScrollPane} container the display appropriately each row. 
	 * <br>
	 * Internally, all the heights are stored in a heights-list.
	 *  
	 * @param targetIndex the index of the i<sup>th</sup> GUI row, whose height will be declared
	 * to the Dynamic Rows Loader API. The bigger value for this parameter is the size of the 
	 * aforementioned heights-list; if it were the case, this would mean we are adding a value
	 * at the end of the heights-list. If this parameter is bigger than the heights-list size 
	 * (this is, a number bigger that the rows whose heights have been registered up until now) or
	 * less than 0, this methods returns without performing any action.
	 * @param height the height for the i<sup>th</sup> GUI row. If its value is less that zero
	 * then it will be reset to zero.
	 */
	public void setPositionHeight(int targetIndex, int height) {
		if (targetIndex < 0 || targetIndex > heightsList.size()) {return;}
		int lastSetIndex = heightsList.size() - 1;
		height = height < 0 ? 0 : height;
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
		if (doesDisplayRequireUpdate()) {
			scheduleUpdateVisibleComponentsTask(screenWentDown);		
		}
	}
	
	
	private boolean doesDisplayRequireUpdate() {
		boolean updateRequired = false;
		Rectangle currentVisibleRectangle = resizableViewPortScroll.getViewport().getViewRect();
		if (visibleScreenRectangle == null) {
			visibleScreenRectangle = currentVisibleRectangle;	
			updateRequired = true;
		} else {			
			Rectangle intersectionRectangle = visibleScreenRectangle.intersection(currentVisibleRectangle);
			intersectionRectangle.setSize(visibleScreenRectangle.width, intersectionRectangle.height);
			Rectangle widthAdjustedComparissionRectangle = currentVisibleRectangle.getBounds();
			widthAdjustedComparissionRectangle.setSize(visibleScreenRectangle.width, 
					widthAdjustedComparissionRectangle.height);
			if (!widthAdjustedComparissionRectangle.equals(intersectionRectangle)) {
				updateRequired = true;
				visibleScreenRectangle = currentVisibleRectangle;	
			}
		}		
		return updateRequired;
	}
	
	
	/**
	 * Requests the execution of a GUI update task. This task is responsible for creating the GUI 
	 * rows (by using the {@link ToDisplayComponentRenderer} object provided during the 
	 * construction of this API) and displaying them in the correct position of the JScrollPane.
	 * <br>
	 * Typically, this method should be called in two types of occasions. First, after all the 
	 * rows'
	 * heights have been registered with the method {@link #setPositionHeight} for the first time 
	 * and we want to display the correctly positioned analogous GUI rows. Second, after the height 
	 * of one or more rows have updated its height and we need to visually reflect this fact.
	 * <br>
	 * Every time this method is called, a new {@link Thread}, that we will call 
	 * row-update-thread, is created; it has the task of 
	 * performing all calculations necessaries to update appropriately the GUI. This last task
	 * is scheduled on the event dispatch thread at the end of the work of the row-update-thread.
	 * <br>
	 * When a new row-update-thread is started, any other thread of this type that could still
	 * be active, is terminated. 
	 * 
	 * @param screenWentDown whether the JScrollPane was scrolled down. If true, the GUI row
	 * components will be added from top to down; otherwise, they will be added from bottom to
	 * top.
	 */
	public void scheduleUpdateVisibleComponentsTask(boolean screenWentDown) {
		displayViewUpdateTaskScheduler.getPreparedDisplayViewUpdateTaskBuilder()
				.setToDisplayComponentRenderer(toDisplayComponentRenderer)  
        		.setHeightsList(heightsList)
        		.setPointsList(pointsList)
        		.setScreenWentDown(screenWentDown) 
				.callDisplayViewUpdateTask();
	}
	
	
	/**
	 * Resets the state of the Dynamic Rows Loader API. This means that all the GUI components are 
	 * removed from the JScrollPane and the register of positions and heights of each row is 
	 * cleared
	 */
	public void clearState() {
		resizableViewPortScroll.resetScrollSize();
		displayViewUpdateTaskScheduler.clearState();
		visibleScreenRectangle = null;
		lastScreenPosition = 0;
		heightsList.clear();
		pointsList.clear();			
		clearComponentsViewDisplay();
	}
	
	
	private void clearComponentsViewDisplay() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				resizableViewPortScroll.resetScrollSize();
			}
		});
	}


	/**
	 * Returns The {@link JScrollPane} that has been prepared to gradually display GUI 
	 * components. This scroll pane is prepared to respond to scrolling events and request the
	 * update of the currently visible components
	 * 
	 * @return The prepared JScrollPane. This component does not possess any special public
	 * method or property. 
	 */
	public JScrollPane getComponentsDisplayScroll() {		
		return resizableViewPortScroll;
	}
	
	
	/**
	 * Moves the view port of the JScrollPane provided by the Dynamic Rows Loader API. After
	 * this method is called, the i<sup>th</sup> GUI row is displayed in the visible portion of 
	 * the scroll pane.
	 * 
	 * @param componentIndex the index of the GUI row that need to be displayed in the visible 
	 * portion of the scroll pane.
	 * 
	 * @throws IndexOutOfBoundsException if the componentIndex is less than zero or bigger that
	 * the number of registered rows minus one.
	 */
	public void moveViewPortToComonentAtIndex(int componentIndex) {		
		Point location = pointsList.get(componentIndex);
		resizableViewPortScroll.getVerticalScrollBar().setValue(location.y);
	}
	
	
	/**
	 * Traverses each of the {@link Component}s currently displayed on the screen and feeds
	 * them to the indicated {@link Consumer}.
	 * 
	 * @param componentProcessor the object that will be fed each of the Components visible to
	 * perform the corresponding actions. If null, no action will be performed.
	 */
	public void visitVisibleComponents(Consumer<Component> componentProcessor) {
		if (componentProcessor == null) {return;}
		Container container = (Container) resizableViewPortScroll.getViewport().getView();
		Component[] visibleComponents = container.getComponents();
		for (Component component : visibleComponents) {
			componentProcessor.accept(component);
		}
	}


	/**
	 * Adds a component that will be notified when the API is done updating the GUI. Such
	 * events start every time the method {@link #updateScreenOnVisibleDisplayChange} is called.
	 * <br>
	 * This method only notifies interested parties in case the thread scheduled by the 
	 * updateScreenOnVisibleDisplayChange method completes successfully. Threads cancelled before
	 * completion will not derive in a notification to {@link DisplayUpdateTaskOverListener}s.
	 * 
	 * @param displayUpdateTaskOverListener the components that will be notified when every time
	 * a GUI update Thread completes its task.
	 * 
	 * @see {@link Thread}
	 * 
	 */
	public void setDisplayUpdateTaskOverListener(DisplayUpdateTaskOverListener displayUpdateTaskOverListener) {
		displayViewUpdateTaskScheduler.setDisplayUpdateTaskOverListener(displayUpdateTaskOverListener);
	}
}
