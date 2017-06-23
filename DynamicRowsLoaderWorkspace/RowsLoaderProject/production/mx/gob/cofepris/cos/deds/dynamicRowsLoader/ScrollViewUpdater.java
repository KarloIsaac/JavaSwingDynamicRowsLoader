package mx.gob.cofepris.cos.deds.dynamicRowsLoader;

import java.awt.*;
import java.lang.reflect.*;
import javax.swing.*;


class ScrollViewUpdater {
	private ResizableViewPortScroll targetDisplayerScroll;
	private Component targetComponent;
	private int inicialSpaceHeight;
	private int finalSpaceHeight;
	private boolean screenWentDown;


	ScrollViewUpdater setTargetDisplayerScroll(ResizableViewPortScroll targetDisplayerScroll) {
		this.targetDisplayerScroll = targetDisplayerScroll;
		return this;
	}


	ScrollViewUpdater setTargetComponent(Component targetComponent) {
		this.targetComponent = targetComponent;
		return this;
	}


	ScrollViewUpdater setInicialSpaceHeight(int inicialSpaceHeight) {
		this.inicialSpaceHeight = inicialSpaceHeight;
		return this;
	}


	ScrollViewUpdater setFinalSpaceHeight(int finalSpaceHeight) {
		this.finalSpaceHeight = finalSpaceHeight;
		return this;
	}


	ScrollViewUpdater setScreenWentDown(boolean screenWentDown) {
		this.screenWentDown = screenWentDown;
		return this;
	}


	void updateScrollView() {
		testValidUpdateState();
		try {
			Runnable scrollUpdateThread = createScrollUpdateRunnable();
			SwingUtilities.invokeAndWait(scrollUpdateThread);
		} catch (InterruptedException e) {
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}


	private Runnable createScrollUpdateRunnable() {
		return new Runnable() {
			@Override
			public void run() {
				targetDisplayerScroll.setInicialAndFinalSpaceAdjustHeight(inicialSpaceHeight, finalSpaceHeight);
				targetDisplayerScroll.addComponentToViewDisplay(targetComponent, screenWentDown);
				targetDisplayerScroll.revalidate();
				targetDisplayerScroll.repaint();
			}
		};
	}


	private void testValidUpdateState() {
		if (targetDisplayerScroll == null) {
			throw new IllegalStateException();
		} else if (targetComponent == null) {
			throw new IllegalStateException();
		} else if (inicialSpaceHeight < 0) {
			throw new IllegalStateException();
		} else if (finalSpaceHeight < 0) {
			throw new IllegalStateException();
		}
	}
}