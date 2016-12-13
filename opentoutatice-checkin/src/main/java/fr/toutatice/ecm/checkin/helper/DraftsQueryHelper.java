/**
 * 
 */
package fr.toutatice.ecm.checkin.helper;


/**
 * @author david
 *
 */
public interface DraftsQueryHelper {
    
    /** Drafts query. */
    String DRAFTS_QUERY_OF = "select * from Document where ecm:mixinType = \"OttcDraft\" "
            + "and ottcDft:checkoutParentId in %s and ecm:currentLifeCycleState = 'deleted' and ecm:isProxy = 0 and ecm:isVersion = 0";

    /** Orphan Drafts query. */
    String ORPHAN_DRAFTS_QUERY_OF = "select * from Document where ecm:mixinType = \"OttcDraft\" "
            + "and ottcDft:checkinedDocId = \"\" and ottcDft:checkoutParentId = \"%s\" and ecm:isProxy = 0 and ecm:isVersion = 0";
    
    /** Key to store webIds in documentEventContext. */
    String PARENT_CHECKOUT_IDS_LIST = "parentCheckoutIds";

}
