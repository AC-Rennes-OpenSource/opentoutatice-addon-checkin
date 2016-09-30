/**
 * 
 */
package fr.toutatice.ecm.checkin.listener;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventContext;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;
import fr.toutatice.ecm.checkin.helper.DocumentHelper;


/**
 * @author david
 *
 */
public class MoveCheckinedListener implements EventListener {

    /**
     * Constructor.
     */
    public MoveCheckinedListener() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void handleEvent(Event event) throws ClientException {
        EventContext context = event.getContext();
        if (DocumentEventTypes.DOCUMENT_MOVED.equals(event.getName()) && context instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) context;
            DocumentModel srcDoc = docCtx.getSourceDocument();
            CoreSession session = docCtx.getCoreSession();
            
            DocumentCheckinHelper checkinHelper = DocumentCheckinHelper.getInstance();
            if(checkinHelper.hasDraft(srcDoc)){
                DocumentModel checkinedParent = session.getParentDocument(srcDoc.getRef());
                DocumentModel draft = checkinHelper.getDraftDoc(session, srcDoc);
                
                checkinHelper.setCheckinedParentId(draft, checkinedParent);
                session.saveDocument(draft);
                session.save();
            }
        }
    }

}
