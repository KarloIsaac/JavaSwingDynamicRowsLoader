package mx.gob.cofepris.cos.deds.dynamicRowsLoader;

import java.awt.Component;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


class DisplayViewUpdateTaskScheduler {
	private ExecutorService componentDisplayExecutorService = Executors.newSingleThreadExecutor();
	private Future<?> lastDisplayUpdateTaskSubmmited;
	private HashMap<Integer, Component> currentDisplayedComponentsCacheMap = new HashMap<>();
	private DisplayUpdateTaskOverListener displayUpdateTaskOverListener;
	private ResizableViewPortScroll resizableViewPortScroll;

	
	DisplayViewUpdateTaskScheduler(ResizableViewPortScroll resizableViewPortScroll) {
		if (resizableViewPortScroll == null) {
			throw new IllegalArgumentException("the resizableViewPortScroll cannot be null");
		}
		this.resizableViewPortScroll = resizableViewPortScroll;
	}
	
	
	public void setDisplayUpdateTaskOverListener(DisplayUpdateTaskOverListener displayUpdateTaskOverListener) {
		this.displayUpdateTaskOverListener = displayUpdateTaskOverListener;		
	}

	
	public void clearState() {		
		stopLastDisplayUpdateTask();
		lastDisplayUpdateTaskSubmmited = null;
		currentDisplayedComponentsCacheMap.clear();
	}


	public DisplayViewUpdateTaskBuilder getPreparedDisplayViewUpdateTaskBuilder() {
		stopLastDisplayUpdateTask();
		return new DisplayViewUpdateTaskBuilder();
	}


	private synchronized void submmitDisplayViewUpdateTask(DisplayViewUpdateTaskBuilder displayViewUpdateTaskBuilder) {
		stopLastDisplayUpdateTask();
		lastDisplayUpdateTaskSubmmited = componentDisplayExecutorService.submit(
				buildDisplayViewUpdateTask(displayViewUpdateTaskBuilder));
	}


	private void stopLastDisplayUpdateTask() {
		if (lastDisplayUpdateTaskSubmmited != null) {
			lastDisplayUpdateTaskSubmmited.cancel(true);
		}
	}


	private Runnable buildDisplayViewUpdateTask(final DisplayViewUpdateTaskBuilder displayViewUpdateTaskBuilder) {
		Runnable displayViewUpdateTask = new Runnable() {
			@Override
			public void run() {
				try {
					DisplayViewUpdateTask displayViewUpdateTask = 
							displayViewUpdateTaskBuilder.buildDisplayViewUpdateTask();
					displayViewUpdateTask.setDisplayUpdateTaskOverListener(displayUpdateTaskOverListener);
					displayViewUpdateTask.displayComponentsInViewRange();
				} catch (Throwable e) {
					e.printStackTrace();
					throw e;
				}
			}
		};
		return displayViewUpdateTask;
	}

	
	public class DisplayViewUpdateTaskBuilder {
		private ToDisplayComponentRenderer toDisplayComponentRenderer;
		
		private ArrayList<Integer> heightsList;
		private ArrayList<Point> pointsList;
		private boolean screenWentDown;
		private boolean isScreenWentDownSet = false;


		private DisplayViewUpdateTaskBuilder() {}


		public DisplayViewUpdateTaskBuilder setToDisplayComponentRenderer(
				ToDisplayComponentRenderer toDisplayComponentRenderer) {
			this.toDisplayComponentRenderer = toDisplayComponentRenderer;
			return this;
		}


		public DisplayViewUpdateTaskBuilder setHeightsList(ArrayList<Integer> heightsList) {
			if (heightsList != null) {
				ArrayList<Integer> cloneList = new ArrayList<>();
				for (int originalHeight : heightsList) {
					cloneList.add(new Integer(originalHeight));
				}
				this.heightsList = cloneList;
			}
			return this;
		}


		public DisplayViewUpdateTaskBuilder setPointsList(ArrayList<Point> pointsList) {
			if (pointsList != null) {
				ArrayList<Point> cloneList = new ArrayList<>();
				for (Point originalPoint : pointsList) {
					Point pointClone = new Point(originalPoint);
					cloneList.add(pointClone);
				}
				this.pointsList = cloneList;
			}
			return this;
		}


		public DisplayViewUpdateTaskBuilder setScreenWentDown(boolean screenWentDown) {
			isScreenWentDownSet = true;
			this.screenWentDown = screenWentDown;
			return this;
		}


		public void callDisplayViewUpdateTask() {
			DisplayViewUpdateTaskScheduler.this.submmitDisplayViewUpdateTask(this);
		}


		private DisplayViewUpdateTask buildDisplayViewUpdateTask() {
			DisplayViewUpdateTask displayViewUpdateTask = new DisplayViewUpdateTask();
			displayViewUpdateTask.toDisplayComponentRenderer = toDisplayComponentRenderer;
			displayViewUpdateTask.displayedComponentsCache = currentDisplayedComponentsCacheMap;
			displayViewUpdateTask.displayerScroll = resizableViewPortScroll;
			displayViewUpdateTask.heightsList = heightsList;
			displayViewUpdateTask.pointsList = buildPointsList();
			displayViewUpdateTask.screenWentDown = screenWentDown;
			checkValidConstructionState(displayViewUpdateTask);
			return displayViewUpdateTask;
		}


		private ArrayList<Point> buildPointsList() {
			ArrayList<Point> cloneList = new ArrayList<>();
			for (Point originalPoint : pointsList) {
				Point pointClone = new Point(originalPoint);
				cloneList.add(pointClone);
			}
			return cloneList;
		}


		private void checkValidConstructionState(DisplayViewUpdateTask displayViewUpdateTask) {
			if (displayViewUpdateTask.toDisplayComponentRenderer == null) {
				throw new IllegalStateException();
			} else if (displayViewUpdateTask.displayedComponentsCache == null) {
				throw new IllegalStateException();
			} else if (displayViewUpdateTask.displayerScroll == null) {
				throw new IllegalStateException();
			} else if (!isScreenWentDownSet) {
				throw new IllegalStateException();
			}
		}
	}
}