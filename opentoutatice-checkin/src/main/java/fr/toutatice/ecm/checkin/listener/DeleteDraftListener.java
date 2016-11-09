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
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.LifeCycleConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventTypes;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventListener;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;

import fr.toutatice.ecm.checkin.helper.DocumentCheckinHelper;
import fr.toutatice.ecm.platform.core.helper.ToutaticeDocumentHelper;
import fr.toutatice.ecm.platform.core.helper.ToutaticeSilentProcessRunnerHelper;
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
     * Restore state of checkined document when its draft is deleted.
     * Removes Draft when checkined document is put in trash.
     */
    @Override
    public void handleEvent(Event event) {
        if (event.getContext() instanceof DocumentEventContext) {
            DocumentEventContext docCtx = (DocumentEventContext) event.getContext();

            // Case of Draft removed
            if (DocumentEventTypes.ABOUT_TO_REMOVE.equals(event.getName())) {
                DocumentModel srcDoc = docCtx.getSourceDocument();
                if (srcDoc.hasFacet(DRAFT_FACET)) {
                    CoreSession session = docCtx.getCoreSession();
                    String checkinedId = DocumentCheckinHelper.getInstance().getCheckinedId(srcDoc);

                    if (StringUtils.isNotEmpty(checkinedId)) {
                        // Draft has a checkined document
                        restoreCheckinedDoc(session, checkinedId);
                    }
                }
            }
            // Case of checkined document put in Draft
            if (LifeCycleConstants.TRANSITION_EVENT.equals(event.getName())) {
                Map<String, Serializable> properties = docCtx.getProperties();
                if (properties != null) {
                    String transition = (String) properties.get(LifeCycleConstants.TRANSTION_EVENT_OPTION_TRANSITION);
                    if (LifeCycleConstants.DELETE_TRANSITION.equals(transition)) {
                        DocumentModel srcDoc = docCtx.getSourceDocument();
                        CoreSession session = docCtx.getCoreSession();
                        
                        DocumentModel draftDoc = DocumentCheckinHelper.getInstance().getDraftDoc(session, srcDoc);
                        if(draftDoc != null){
                            ToutaticeDocumentHelper.removeDocumentSilently(session, draftDoc, true);
                        }
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

        if (StringUtils.isNotEmpty(checkinedId)) {
            DocumentModel checkinedDoc = WebIdResolver.getLiveDocumentByWebId(session, checkinedId);

            if (checkinedDoc != null) {
                checkinedDoc.removeFacet(CHECKINED_IN_FACET);
                session.removeLock(checkinedDoc.getRef());
                ToutaticeDocumentHelper.saveDocumentSilently(session, checkinedDoc, false);
            } else {
                if (StringUtils.isNotEmpty(checkinedId)) {
                    log.error("No Draft document with webid: ".concat(checkinedId));
                }
            }
        }
    }

}
