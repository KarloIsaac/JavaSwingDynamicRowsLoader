package mx.gob.cofepris.cos.deds.dynamicRowsLoader;

import java.awt.*;
import javax.swing.*;
import net.miginfocom.swing.*;


class ResizableViewPortScroll extends JScrollPane {
	private static final long serialVersionUID = 1L;
	private JPanel componentsViewDisplay;
	private MigLayout migLayout = new MigLayout("insets 0, flowy, gap 0 0");


	public ResizableViewPortScroll() {
		componentsViewDisplay = new JPanel();
		componentsViewDisplay.setLayout(migLayout);
		this.setViewportView(componentsViewDisplay);
		this.getVerticalScrollBar().setUnitIncrement(25);
		this.getHorizontalScrollBar().setUnitIncrement(50);
	}

	
	public void resetScrollSize() {	
		this.getVerticalScrollBar().setValue(0);		
		componentsViewDisplay.removeAll();
		componentsViewDisplay.revalidate();	
		this.revalidate();
		migLayout = new MigLayout("insets 0, flowy, gap 0 0");
		componentsViewDisplay.setLayout(migLayout);
	}

	
	public void clearComponentsViewDisplay() {
		componentsViewDisplay.removeAll();
		setInicialAndFinalSpaceAdjustHeight(componentsViewDisplay.getHeight(), 0);
	}


	public void setInicialAndFinalSpaceAdjustHeight(int initialHeight, int finalHeight) {		
		String constraints = (String) migLayout.getLayoutConstraints();
		constraints = constraints.replaceFirst("insets.*?,", "insets " + initialHeight + " 0 " + finalHeight + " 0,");
		migLayout.setLayoutConstraints(constraints);		
	}
	
	
	public void addComponentToViewDisplay(Component targetComponent, boolean addToEnd) {
		Component[] addedComponentsArray = componentsViewDisplay.getComponents();
		componentsViewDisplay.removeAll();
		if (!addToEnd) {componentsViewDisplay.add(targetComponent);}
		for (Component componentToAdd : addedComponentsArray) {
			componentsViewDisplay.add(componentToAdd);
		}
		if (addToEnd) {componentsViewDisplay.add(targetComponent);}
	}

}