package com.journaldev.spring;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.splunk.*;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;

@RestController
public class PersonController {
	
	@RequestMapping("/")
	public String healthCheck() throws Exception {
		Service service = createService();
		connectToSplunk(service);
		displayDataInputs(service);
		displayAllIndexes(service);
		//createNewIndex(service);
		viewAndModifyIndexProperties(service);
		addDataDirectlyToIndex(service);
		addDataDirectlyToIndexOverHttp(service);
		addDataDirectlyToIndexOverSocket(service);
		return "Welcome to Spring Boot REST...";
	}
	
	public Service createService() {
		//avoids java.lang.RuntimeException: No appropriate protocol
		HttpService.setSslSecurityProtocol(SSLSecurityProtocol.TLSv1_2);
		
        // Create a map of arguments and add login parameters
        ServiceArgs loginArgs = new ServiceArgs();
        loginArgs.setUsername("username");
        loginArgs.setPassword("password");
        loginArgs.setHost("localhost");
        loginArgs.setPort(8089);
        loginArgs.setScheme("https");
        
        // Create a Service instance and log in with the argument map
        Service service = Service.connect(loginArgs);
        return service;
	}
	
	public void connectToSplunk(Service service) {
		
        // Print installed apps in Splunk to the console to verify login
        for (Application app : service.getApplications().values()) {
            System.out.println(app.getName());
        }
    }
	
	public void displayDataInputs(Service service) {
		
        // Get the collection of data inputs
        InputCollection myInputs = service.getInputs();

        // Iterate and list the collection of inputs        
        System.out.println("There are " + myInputs.size() + " data inputs:\n");
        for (Input entity: myInputs.values()) {
            System.out.println("  " + entity.getName() + " (" + entity.getKind() + ")");
        }
    }
	
	public void displayAllIndexes(Service service) {
		
     // Retrieve the collection of indexes, sorted by number of events
        IndexCollectionArgs indexcollArgs = new IndexCollectionArgs();
        indexcollArgs.setSortKey("totalEventCount");
        indexcollArgs.setSortDirection(IndexCollectionArgs.SortDirection.DESC);
        IndexCollection myIndexes = service.getIndexes(indexcollArgs);

        // List the indexes and their event counts
        System.out.println("There are " + myIndexes.size() + " indexes:\n");
        for (Index entity: myIndexes.values()) {
            System.out.println("  " + entity.getName() + " (events: " 
                    + entity.getTotalEventCount() + ")");
        }
    }
	
	public void createNewIndex(Service service) {
        
        //Get the collection of indexes
        IndexCollection myIndexes = service.getIndexes();
        System.out.println("myIndexes size : " + myIndexes.size());
        //Create a new index
        Index myIndex = myIndexes.create("test_index_2");
        
        IndexCollection myIndexes2 = service.getIndexes();
        System.out.println("myIndexes new size : " + myIndexes2.size());
    }
	
    public void viewAndModifyIndexProperties(Service service) {
    	// Retrieve the index that was created earlier
    	Index myIndex = service.getIndexes().get("test_index");

    	// Retrieve properties      
    	System.out.println("Name:                " + myIndex.getName());
    	System.out.println("Current DB size:     " + myIndex.getCurrentDBSizeMB() + "MB");
    	System.out.println("Max hot buckets:     " + myIndex.getMaxHotBuckets());
    	System.out.println("# of hot buckets:    " + myIndex.getNumHotBuckets());
    	System.out.println("# of warm buckets:   " + myIndex.getNumWarmBuckets());
    	System.out.println("Max data size:       " + myIndex.getMaxDataSize());
    	System.out.println("Max total data size: " + myIndex.getMaxTotalDataSizeMB() + "MB");

    	// Modify a property and update the server
    	myIndex.setMaxTotalDataSizeMB(myIndex.getMaxTotalDataSizeMB()-1);
    	myIndex.update();
    	System.out.println("Max total data size: " + myIndex.getMaxTotalDataSizeMB() + "MB");   
    }
    
    public void addDataDirectlyToIndex(Service service) {
    	// Retrieve the index for the data
    	Index myIndex = service.getIndexes().get("test_index");

    	// Specify a file and upload it
    	String uploadme = "C:/Input/server.log";
    	myIndex.upload(uploadme);    	
    }
    
    public void addDataDirectlyToIndexOverHttp(Service service) {
    	// Retrieve the index for the data
    	Index myIndex = service.getIndexes().get("test_index");

    	// Specify  values to apply to the event
    	Args eventArgs = new Args();
    	eventArgs.put("sourcetype", "access_combined.log");
    	eventArgs.put("host", "local");

    	// Submit an event over HTTP
    	myIndex.submit(eventArgs, "This is my HTTP event");
    }
    
    public void addDataDirectlyToIndexOverSocket(Service service) throws Exception {
    	// Retrieve the index for the data
    	Index myIndex = service.getIndexes().get("test_index");

    	// Set up a timestamp
    	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
    	String date = sdf.format(new Date());

    	// Open a socket and stream
    	Socket socket = myIndex.attach();
    	try {
    	     OutputStream ostream = socket.getOutputStream();
    	     Writer out = new OutputStreamWriter(ostream, "UTF8");

    	     // Send events to the socket then close it
    	     out.write(date + "Event one!\r\n");
    	     out.write(date + "Event two!\r\n");
    	     out.flush();
    	} finally {
    	     socket.close();
    	}
    }
        
}
