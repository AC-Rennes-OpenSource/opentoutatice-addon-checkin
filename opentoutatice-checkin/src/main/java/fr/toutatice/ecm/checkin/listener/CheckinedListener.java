/**
 * 
 */
package fr.toutatice.ecm.checkin.listener;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.model.NoSuchDocumentException;

import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;
import fr.toutatice.ecm.checkin.helper.TransitionHelper;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;


/**
 * @author david
 *
 */
public class CheckinedListener implements EventListener {

    /**
     * Constructor.
     */
    public CheckinedListener() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(Event event) throws ClientException {
    	EventContext context = event.getContext();
    	if (context instanceof DocumentEventContext) {

    		DocumentEventContext docCtx = (DocumentEventContext) context;
    		DocumentModel srcDoc = docCtx.getSourceDocument();
    		CoreSession session = docCtx.getCoreSession();

    		DocumentCheckinHelper checkinHelper = DocumentCheckinHelper.getInstance();
    		try {
    			if (checkinHelper.hasDraft(srcDoc)) {
    				if (DocumentEventTypes.DOCUMENT_MOVED.equals(event.getName())) {
    					updateDraft(srcDoc, session, checkinHelper);
    				} else if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName())) {
    					// In case of trashed / untrashed Folderidh, transition event is propagated on its children
    					// (cf BulkLifecCycleListener)
    					String transition = TransitionHelper.getTransition(docCtx, event);
    					if(StringUtils.isNotEmpty(transition)){
    						updateDraftLifeCycle(session, srcDoc, transition);
    					}
    				} else if(DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())){ 
    					removeDraft(session, srcDoc);
    				} else if (DocumentEventTypes.DOCUMENT_CREATED_BY_COPY.equals(event.getName())) {
    					checkinHelper.restoreCheckinedDoc(session, srcDoc);
    				}
    			}
    		} catch (NoSuchDocumentException e) {
    			throw new ClientException(e);
    		}
    	}
    }
    

    /**
     * Updates Draft according to moved checkined document.
     * 
     * @param srcDoc
     * @param session
     * @param checkinHelper
     * @throws NoSuchDocumentException 
     */
    public void updateDraft(DocumentModel srcDoc, CoreSession session, DocumentCheckinHelper checkinHelper) throws NoSuchDocumentException {
        DocumentModel checkinedParent = session.getParentDocument(srcDoc.getRef());
        DocumentModel draft = checkinHelper.getDraftDoc(session, srcDoc);
        
        checkinHelper.setCheckinedParentId(draft, checkinedParent);
        ToutaticeDocumentHelper.saveDocumentSilently(session, draft, true);
    }
    
    /**
     * Delete draft of given checkined document
     * (set in deleted state).
     * 
     * @param checkinedDoc 
     * @throws NoSuchDocumentException 
     */
    private void updateDraftLifeCycle(CoreSession session, DocumentModel checkinedDoc, String transition) throws NoSuchDocumentException {
        DocumentModel draft = DocumentCheckinHelper.getInstance().getDraftDoc(session, checkinedDoc);
        session.followTransition(draft, transition);
    }
    
    /**
     * Remove draft of given checkined document
     * (set in deleted state).
     * 
     * @param checkinedDoc 
     * @throws NoSuchDocumentException 
     */
    private void removeDraft(CoreSession session, DocumentModel checkinedDoc) throws NoSuchDocumentException {
        DocumentModel draft = DocumentCheckinHelper.getInstance().getDraftDoc(session, checkinedDoc);
        // Not done silently for DraftListener to be called
        // to clean checkined document
        ToutaticeDocumentHelper.removeDocumentSilently(session, draft, false);
        // Cache invalidation
        session.save();
    }

}
