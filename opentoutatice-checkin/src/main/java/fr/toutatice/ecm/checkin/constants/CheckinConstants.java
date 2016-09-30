/**
 * 
 */
package fr.toutatice.ecm.checkin.constants;

import fr.toutatice.ecm.platform.core.constants.ToutaticeNuxeoStudioConst;


/**
 * @author david
 *
 */
public interface CheckinConstants {
    
    /** Tags a document as checked in. */
    String CHECKINED_IN_FACET = "OttcCheckedIn";
    /** Tags a document as draft. */
    String DRAFT_FACET = "OttcDraft";
    /** Facet indicating that webId can not be modified. */
    String WEBID_DISABLED_FACET = "WebidDisabled";
    
    /** Draft webid prefix. */
    String DRAFT_PREFIX = "draft_";
    /** Default chekin folder. */
    String DRAFTS = "drafts";
    /** Default chekin folder. */
    String DRAFTS_TITLE = "Mes brouillons";

    /** createdAsDraftParentPath property key. */
    String CHECKINED_PARENT_ID = "ottcDft:checkoutParentId";
    /** targetCheckoutedId property key. */
    String CHECKINED_DOC_ID = "ottcDft:checkinedDocId";
    
    /** draftPath property key. */
    String DRAFT_PATH = "ottcChk:draftPath";
    /** draftId property key. */
    String DRAFT_ID = "ottcChk:draftId";
    
    /** Webid property key. */
    String OTTC_WEBID = ToutaticeNuxeoStudioConst.CST_DOC_SCHEMA_TOUTATICE_WEBID;
    
    /** Key Portal Notification */
    String SUCCESS_MESSAGE_DRAFT_CREATED = "SUCCESS_MESSAGE_DRAFT_CREATED";

}
