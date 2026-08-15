package com.stock.ticker.model;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public record WCRatioResponse(
        String note,
        List<SalesToWC> salesToWCList
) {
    public static WCRatioResponse getWCRatioResponse(List<SalesToWC> salesToWCList){
        String note = """
                WC Ratio can increase or decrease due to sales Revenue(Total Revenue). Sales Revenue going down is not always bad for the company.
                Sales Revenue can go down due to the following reasons
                1. Company has reduced its inventory levels.
                2. Company has tightened its credit policies.
                
                Sales Revenue can go up due to the following reasons
                1. Company has increased its inventory levels.
                2. Company has tightened its credit policies. 
                
                Company may want to reduce inventory levels if they feel they want to invest the money elsewhere. 
                
                The tightening of credit policy is usually depicted by amount receivables going down. 
                The inventory levels going down is depicted by the inventory figures going down. 
                 
                It is best to avoid companies with negative WC Ratio. 
                """;

        Collections.reverse(salesToWCList);
        return new WCRatioResponse(note, salesToWCList);
    }
}
