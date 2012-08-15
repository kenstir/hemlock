package org.evergreen.android.searchCatalog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opensrf.util.OSRFObject;

public class RecordInfo implements Serializable {

    // {"title","author","doc_id","doc_type","pubdate","isbn","publisher","tcn","subject","type_of_resources","call_numbers","edition","online_loc","synopsis","physical_description","toc","copy_count","series","serials","foreign_copy_maps"});

    /**
	 * 
	 */
    private static final long serialVersionUID = 10123L;

    public String title = null;

    public String author = null;

    public String pubdate = null;

    public String isbn = null;

    public Integer doc_id = null;

    public String publisher = null;

    public String subject = null;

    public String doc_type = null;

    public String online_loc = null;

    public String synopsis = null;

    public String physical_description = null;

    public String series = null;

    // tcn field
    public String image = null;

    public boolean dummy = false;

    public ArrayList<CopyCountInformation> copyCountListInfo = null;

    public List<CopyInformation> copyInformationList = null;

    public RecordInfo() {
        this.title = "Test title";
        this.author = "Test author";
        this.pubdate = "Publication date";
        copyInformationList = new ArrayList<CopyInformation>();

        // marks the fact that this is a record made from no info
        this.dummy = true;
    }

    public RecordInfo(OSRFObject info) {
        copyInformationList = new ArrayList<CopyInformation>();
        try {

            this.title = info.getString("title");
            this.author = info.getString("author");
            this.pubdate = info.getString("pubdate");
            this.publisher = info.getString("publisher");
            this.doc_id = info.getInt("doc_id");
            this.image = info.getString("tcn");
            this.doc_type = info.getString("doc_type");
        } catch (Exception e) {
            System.out.println("Exception basic info " + e.getMessage());
        }
        ;

        try {
            this.isbn = (String) info.get("isbn");
        } catch (Exception e) {
            System.out.println("Exception isbn " + e.getMessage());
        }
        ;

        try {

            Map<String, ?> subjectMap = (Map<String, ?>) info.get("subject");

            this.subject = "";

            int no = subjectMap.entrySet().size();
            int i = 0;
            for (Entry<String, ?> entry : subjectMap.entrySet()) {
                i++;
                if (i < no)
                    this.subject += entry.getKey() + " \n";
                else
                    this.subject += entry.getKey();
            }

        } catch (Exception e) {
            System.out.println("Exception subject " + e.getMessage());
        }
        ;
        try {

            this.online_loc = ((List) info.get("online_loc")).get(0).toString();

        } catch (Exception e) {
            System.out.println("Exception online_loc " + e.getMessage());
        }
        ;
        try {
            this.physical_description = (String) info
                    .get("physical_description");
        } catch (Exception e) {
            System.out.println("Exception physical_description "
                    + e.getMessage());
        }
        ;
        try {
            this.series = "";
            List<String> seriesList = (List) info.get("series");
            for (int i = 0; i < seriesList.size(); i++)
                if (i < seriesList.size() - 1)
                    this.series += seriesList.get(i) + ", ";
                else
                    this.series += seriesList.get(i);
        } catch (Exception e) {
            System.out.println("Exception series " + e.getMessage());
        }
        ;

    }
}
