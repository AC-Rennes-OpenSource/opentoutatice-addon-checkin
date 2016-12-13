/**
 * 
 */
package fr.toutatice.ecm.checkin.helper;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;


/**
 * @author david
 *
 */
public class TransitionHelper {

    /**
     * Utility class.
     */
    private TransitionHelper() {};
    
    /**
     * Checks if event corresponds to given life cycle transition.
     * 
     * @param docCtx
     * @param event
     * @return true if corresponds to given life cycle transition
     */
    public static boolean isTransition(DocumentEventContext docCtx, Event event, String checkedTransition) {
        if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName())) {
            Map<String, Serializable> properties = docCtx.getProperties();
            if (properties != null) {
                String transition = (String) properties.get(LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);
                return checkedTransition.equals(transition);
            }
        }
        return false;
    }
    
    /**
     * Gets delete or undelete transition name.
     * 
     * @param docCtx
     * @param event
     * @return delete or undelete transition name
     */
    public static String getTransition(DocumentEventContext docCtx, Event event){
        if(isTransition(docCtx, event, LifeCycleConstants.DELETE_TRANSITION)){
            return LifeCycleConstants.DELETE_TRANSITION;
        } else if(isTransition(docCtx, event, LifeCycleConstants.UNDELETE_TRANSITION)){
            return LifeCycleConstants.UNDELETE_TRANSITION;
        }
        return StringUtils.EMPTY;
    }
    
    /**
     * Gets delete or undelete transition name.
     * 
     * @return delete or undelete transition name
     */
    public static String getTransitionName(boolean isDeletion){
        if(isDeletion){
            return LifeCycleConstants.DELETE_TRANSITION;
        } else {
            return LifeCycleConstants.UNDELETE_TRANSITION;
        }
    }

}
