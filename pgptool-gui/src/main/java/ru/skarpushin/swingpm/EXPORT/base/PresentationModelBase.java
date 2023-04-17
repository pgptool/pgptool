package ru.skarpushin.swingpm.EXPORT.base;

import java.awt.Window;
import java.awt.event.ActionEvent;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.apache.log4j.Logger;
import org.pgptool.gui.ui.root.RootPm;
import org.pgptool.gui.ui.tools.UiUtils;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * @param <H>
 *            Host type
 * @param <P>
 *            Parameter Object type used to initialize the PresentationModelBase
 *            via calling {@link #init(ActionEvent, Object, Object)}
 */
public class PresentationModelBase<H, P> extends ru.skarpushin.swingpm.base.PresentationModelBase {
	private Logger log = Logger.getLogger(getClass());

	@Autowired
	private RootPm rootPm;

	/**
	 * Action that resulted in invocation of this PresentationModelBase
	 */
	protected ActionEvent originAction;
	protected H host;
	protected P initParams;

	@Override
	public Window findRegisteredWindowIfAny() {
		Window ret = super.findRegisteredWindowIfAny();
		if (ret != null) {
			log.debug("findRegisteredWindowIfAny returning own window: " + getWindowName(ret));
			return ret;
		}

		if (originAction != null) {
			ret = UiUtils.findWindow(originAction);
			if (ret != null && ret.isVisible()) {
				log.debug("findRegisteredWindowIfAny returning window from originAction: " + getWindowName(ret));
				return ret;
			}
		}

		ret = rootPm.findMainFrameWindow();
		if (ret != null && ret.isVisible()) {
			log.debug("findRegisteredWindowIfAny returning window from rootPm: " + getWindowName(ret));
			return ret;
		}

		log.debug("findRegisteredWindowIfAny returning null");
		return null;
	}

	protected String getWindowName(Window ret) {
		if (ret instanceof JFrame) {
			return ((JFrame) ret).getTitle();
		}
		if (ret instanceof JDialog) {
			return ((JDialog) ret).getTitle();
		}
		return ret.getName();
	}

	/**
	 * Init presenter by clarifying which action resulted in its invocation (used to
	 * trace origin action and subsequently origin window)
	 * 
	 * @return true if presenter is ok to proceed, or false otherwise
	 */
	public boolean init(ActionEvent originAction, H host, P initParams) {
		this.originAction = originAction;
		this.host = host;
		this.initParams = initParams;
		return true;
	}

}
