/**
 * 
 */
package fr.toutatice.ecm.checkin.listener;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import fr.toutatice.ecm.checkin.constants.CheckinConstants;
import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;


/**
 * @author david
 *
 */
public class CopyCheckinedListener implements EventListener {

    /**
     * Constructor.
     */
    public CopyCheckinedListener() {
       super();
    }
    
    /**
     * Remove chekined informations when a checkined document
     * is copied.
     * 
     * @param event
     */
    @Override
    public void handleEvent(Event event){
        EventContext context = event.getContext();
        if (DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(event.getName()) && context instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) context;
            DocumentModel srcDoc = docCtx.getSourceDocument();
            
            if(DocumentCheckinHelper.getInstance().hasDraft(srcDoc)){
                CoreSession session = docCtx.getCoreSession();
                
                srcDoc.removeFacet(CheckinConstants.CHECKINED_IN_FACET);
                session.saveDocument(srcDoc);
                
                session.removeLock(srcDoc.getRef());
                // Session save cause lock infos are not directly linked to hierarchy table
                session.save();
            }
        }
    }
    
}
