package mx.gob.cofepris.cos.deds.dynamicRowsLoader;

/**
 * Interface to be implemented by all parties interested on being informed of the completion
 * of the {@link MultiRowViewRenditionManager#scheduleUpdateVisibleComponentsTask 
 * GUI updating task}
 * 
 * @author kijimenez
 *
 */
public interface DisplayUpdateTaskOverListener {

	/**
	 * The actions to perform after the GUI has been updated.
	 */
	public void performUpdateTaskIsOverActions();
}
