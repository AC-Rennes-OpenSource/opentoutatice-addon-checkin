/**
 * 
 */
package fr.toutatice.ecm.checkin.listener;

import static fr.toutatice.ecm.checkin.constants.CheckinConstants.CHECKINED_IN_FACET;
import static fr.toutatice.ecm.checkin.constants.CheckinConstants.DRAFT_FACET;

import java.io.Serializable;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventBundle;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import fr.toutatice.ecm.checkin.constants.CheckinConstants;
import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;
import fr.toutatice.ecm.platform.service.url.WebIdResolver;


/**
 * @author david
 *
 */
public class DeleteDraftListener implements EventListener {
    
    /** Log. */
    private static final Log log = LogFactory.getLog(DeleteDraftListener.class);

    /**
     * Constructor.
     */
    public DeleteDraftListener() {
        super();
    }

    /**
     * Restore state of checkined document
     * when its draft is deleted.
     */
    @Override
    public void handleEvent(Event event) {
        if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())) {
            if (event.getContext() instanceof DocumentEventContext) {
                DocumentEventContext docCtx = (DocumentEventContext) event.getContext();
                DocumentModel srcDoc = docCtx.getSourceDocument();
                if (srcDoc.hasFacet(DRAFT_FACET)) {
                    CoreSession session = docCtx.getCoreSession();
                    String checkinedId = DocumentCheckinHelper.getInstance().getCheckinedId(srcDoc);
                    
                    if(StringUtils.isNotEmpty(checkinedId)){
                        // Draft has a checkined document
                        restoreCheckinedDoc (session, checkinedId);
                    }
                }
            }
        }
    }
    
    /**
     * Restore state of checkined document
     * when its draft is deleted.
     * 
     * @param session
     * @param srcDoc
     */
    protected void restoreCheckinedDoc(CoreSession session, String checkinedId) {
        
        if(StringUtils.isNotEmpty(checkinedId)){
            DocumentModel checkinedDoc = WebIdResolver.getLiveDocumentByWebId(session, checkinedId);
            
            if(checkinedDoc != null){
                checkinedDoc.removeFacet(CHECKINED_IN_FACET);
                session.removeLock(checkinedDoc.getRef());
                session.saveDocument(checkinedDoc);
                session.save();
            } else {
                if(StringUtils.isNotEmpty(checkinedId)){
                    log.error("No Draft document with webid: ".concat(checkinedId));
                }
            }
        }
    }

}
