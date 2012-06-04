package org.evergreen.android.searchCatalog;

import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class RecordDetails extends TabActivity{

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
        TabHost tabHost = getTabHost();
        
        
        RecordInfo record = (RecordInfo) getIntent().getSerializableExtra("recordInfo");
        
        // Tab for Photos
        TabSpec photospec = tabHost.newTabSpec("Info");
        // setting Title and Icon for the Tab
        photospec.setIndicator("Info");
        Intent photosIntent = new Intent(this, RecordDetails_Info.class);
        photospec.setContent(photosIntent);
        photosIntent.putExtra("recordInfo", record);
        
        // Tab for Songs
        TabSpec songspec = tabHost.newTabSpec("Content");
        songspec.setIndicator("Content");
        Intent songsIntent = new Intent(this, RecordDetails_Content.class);
        songspec.setContent(songsIntent);
        songsIntent.putExtra("recordInfo", record);
        // Tab for Videos
        TabSpec videospec = tabHost.newTabSpec("Other");
        videospec.setIndicator("Other");
        Intent videosIntent = new Intent(this, RecordDetails_Details.class);
        videospec.setContent(videosIntent);
        videosIntent.putExtra("recordInfo",record);
 
        // Adding all TabSpec to TabHost
        tabHost.addTab(photospec); // Adding photos tab
        tabHost.addTab(songspec); // Adding songs tab
        tabHost.addTab(videospec); // Adding videos tab
		
	}
}
