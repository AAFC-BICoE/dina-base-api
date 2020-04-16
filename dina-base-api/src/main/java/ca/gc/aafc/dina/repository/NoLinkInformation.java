package ca.gc.aafc.dina.repository;

import io.crnk.core.resource.links.LinksInformation;

/**
 * Since CRNK does not have a way not to display links in API responses, this class will serve the
 * purpose of not showing any information in the links.
 * 
 * This is just used to remove the top-level links. To remove the other links in a response you will
 * need to use the crnk-compact header.
 */
public class NoLinkInformation implements LinksInformation {
}
