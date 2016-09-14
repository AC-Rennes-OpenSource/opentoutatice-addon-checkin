/**
 * 
 */
package fr.toutatice.ecm.checkin.helper;

import static fr.toutatice.ecm.checkin.constants.CheckinConstants.DRAFT_PREFIX;
import static fr.toutatice.ecm.checkin.constants.CheckinConstants.OTTC_WEBID;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.pathsegment.PathSegmentService;
import org.nuxeo.runtime.api.Framework;

import fr.toutatice.ecm.checkin.constants.CheckinConstants;
import fr.toutatice.ecm.platform.service.url.WebIdResolver;


/**
 * @author david
 *
 */
public class DocumentHelper {
    
    /**
     * Utility class.
     */
    private DocumentHelper() {
        super();
    }
    
    /**
     * @param document
     * @return webId of document.
     */
    public static String getId(DocumentModel document){
        return (String) document.getPropertyValue(OTTC_WEBID);
    }
    
    /**
     * @param originalDocument
     * @return webId of document prefixed with "draft_".
     */
    public static String getDraftIdFromId(DocumentModel checkinedDocument){
        String webId = (String) checkinedDocument.getPropertyValue(OTTC_WEBID);
        return DRAFT_PREFIX + webId;
    }
    
    /**
     * Sets drfat webId: draft_webId.
     * 
     * @param draft
     */
    public static void setDraftIdFromId(DocumentModel draft){
        String webId = (String) draft.getPropertyValue(OTTC_WEBID);
        if(!webId.startsWith(DRAFT_PREFIX)){
            draft.setPropertyValue(OTTC_WEBID, DRAFT_PREFIX + webId);
        }
    }
    
    /**
     * @param draftDocument
     * @return webid of original document from draft document.
     */
    public static String getIdFromDraftId(DocumentModel draftDocument){
        String draftId = (String) draftDocument.getPropertyValue(OTTC_WEBID);
        if(draftId.startsWith(DRAFT_PREFIX)){
            return StringUtils.replace(draftId, DRAFT_PREFIX, StringUtils.EMPTY);
        }
        return draftId;
    }
    
    /**
     * @param session
     * @param webId
     * @return path from webId.
     */
    public static String getPathFromId(CoreSession session, String webId){
        DocumentModel document = WebIdResolver.getLiveDocumentByWebId(session, webId);
        if(document != null){
            return document.getPathAsString();
        }
        return StringUtils.EMPTY;
    }
    
    /**
     * @param document
     * @return parent path of document.
     */
    public static String getParentPath(DocumentModel document){
        return StringUtils.substringBefore(document.getPathAsString(), "/".concat(document.getName()));
    }
    
    /**
     * @param session
     * @param documentRef
     * @return path of document reference.
     */
    public static String getPath(CoreSession session, DocumentRef documentRef){
        DocumentModel document = session.getDocument(documentRef);
        if(document != null){
            return document.getPathAsString();
        }
        // TODO: Exception?
        return StringUtils.EMPTY;
    }
    
}
