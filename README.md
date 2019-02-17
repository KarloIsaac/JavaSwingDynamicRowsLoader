# JavaSwingDynamicRowsLoader

This project aims to help in the stepped loading of Swing components that represent rows of information.

# Background 

It is a common task to retrieve great quantities of information from an RDMS. Usually, this information will have a tabular format. Now, we have the task of taking each of these registers (rows)  and create a row GUI to visualize and, on occasions, modify the information. Typically, we are going to display the rows inside some kind of scrolling panel.

In case we or the users of the system are retrieving a great amount of information, we are going to face the problem of creating a great deal of GUI rows; this, in turn, will most likely saturate the memory of the computer and make the GUI very slow.

It is clear that the user can only visualize a finite (and small) number of rows at the same time; the number will be determined by the size of the screen and the size of the scroll pane. We can take advantage of this fact to avoid the overhead of creating unnecessary rows.

The problem, then, is reduced to find a way to only display the components at the scrolled position in our container.

# Methodology and dependencies

This project was built having Java Swing components in mind to create the GUI.

This project was built using Java 8. While some previous versions of Java could work, no guarantee is done about it. 

Besides the standard libraries of Java, this project uses [MigLayout](http://www.miglayout.com) as the layout manager for most Containers.

# Use of the project

This project has only 3 public components: the class MultiRowViewRenditionManager and the interfaces ToDisplayComponentRenderer and DisplayUpdateTaskOverListener.

The MultiRowViewRenditionManager is acting as the API of the project; through it, we can return the scroll pane where our rows will be positioned, we can request the update of the GUI, add listeners to listen to the events when the program is done updating the GUI, etc.

The user of this project must provide a ToDisplayComponentRenderer component that has one task: return a Java Component given the index of
said component in the registers grid.

The typical methodology to use the project is as follows:
1. Create a component that implements the ToDisplayComponentRenderer interface.
1. Build a new MultiRowViewRenditionManager object, feeding the ToDisplayComponentRenderer on the constructor.
1.  Set the visual height of each of the GUI rows that will be displayed on the scroll pane, using the method MultiRowViewRenditionManager .setPositionHeight.
1. Schedule the update of the GUI using the MultiRowViewRenditionManager.scheduleUpdateVisibleComponentsTask method. This method must be called each time that the GUI needs to be updated.

The interface DisplayUpdateTaskOverListener is only implemented by components that require to perform some action each time the GUI is done being updated.
